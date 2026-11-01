<?php

namespace App\Console\Commands;

use App\Messaging\Outcome;
use App\Messaging\StatusEventHandler;
use Illuminate\Console\Attributes\Description;
use Illuminate\Console\Attributes\Signature;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\Log;
use PhpAmqpLib\Channel\AMQPChannel;
use PhpAmqpLib\Connection\AMQPStreamConnection;
use PhpAmqpLib\Exception\AMQPTimeoutException;
use PhpAmqpLib\Message\AMQPMessage;
use PhpAmqpLib\Wire\AMQPTable;
use Throwable;

/**
 * Receives the status events from RabbitMQ and applies them to the REST mock. Start: php artisan returns:consume
 *
 * This is the PHP counterpart of the Java module messaging-consumer. It has its OWN queue on the same exchange,
 * so both consumers get every event and neither knows about the other (publish/subscribe). All decisions are made
 * in {@see StatusEventHandler}; this class only talks to the broker.
 *
 * The queue is declared with exactly the dead letter settings of the Java queues (see RabbitConfig.java in
 * messaging-consumer). That is required: a rejected message leaves the queue with the routing key
 * "dead-letter". Without it the dead letter exchange would not route it and the message would vanish.
 */
#[Signature('returns:consume')]
#[Description('Consume ReturnStatusChanged events from RabbitMQ and apply them to the REST mock')]
class ConsumeStatusEvents extends Command
{
    public const EXCHANGE = 'returns.status';

    public const DEAD_LETTER_EXCHANGE = 'returns.status.dlx';

    public const QUEUE = 'returns.status.portal';

    public const DEAD_LETTER_QUEUE = 'returns.status.dlq';

    public const DEAD_LETTER_ROUTING_KEY = 'dead-letter';

    private bool $stop = false;

    public function handle(StatusEventHandler $handler): int
    {
        $config = config('services.rabbitmq');

        try {
            $connection = new AMQPStreamConnection(
                $config['host'], $config['port'], $config['user'], $config['password'],
                heartbeat: 30, read_write_timeout: 65,
            );
        } catch (Throwable $e) {
            $this->error("Cannot connect to RabbitMQ at {$config['host']}:{$config['port']}: {$e->getMessage()}");

            return self::FAILURE;
        }

        $channel = $connection->channel();
        $this->declareTopology($channel);

        // One message at a time: the next one is delivered after the previous one was acknowledged.
        $channel->basic_qos(0, 1, false);
        $channel->basic_consume(self::QUEUE, '', false, false, false, false,
            fn (AMQPMessage $message) => $this->onMessage($handler, $message));

        if (function_exists('pcntl_async_signals')) {
            pcntl_async_signals(true);
            pcntl_signal(SIGINT, fn () => $this->stop = true);
            pcntl_signal(SIGTERM, fn () => $this->stop = true);
        }

        $this->info('Waiting for events on queue '.self::QUEUE.' (Ctrl+C to stop)');

        while (! $this->stop && $channel->is_consuming()) {
            try {
                $channel->wait(null, false, 1);
            } catch (AMQPTimeoutException) {
                // Nothing arrived within one second. Loop again, so the stop flag and the heartbeat are checked.
            }
        }

        $channel->close();
        $connection->close();
        $this->info('Stopped.');

        return self::SUCCESS;
    }

    private function onMessage(StatusEventHandler $handler, AMQPMessage $message): void
    {
        try {
            $outcome = $handler->handle($message->getBody());
        } catch (Throwable $e) {
            // A bug or a database problem. Requeueing could loop forever, so the message goes to the dead letter queue.
            Log::error('Unexpected error while handling a message: '.$e->getMessage(), ['exception' => $e]);
            $this->error("Unexpected error: {$e->getMessage()}");
            $message->nack(false);

            return;
        }

        match ($outcome) {
            Outcome::Processed, Outcome::Duplicate => $message->ack(),
            Outcome::Rejected => $message->nack(false), // requeue = false: the broker dead-letters the message
        };
        $this->line('  '.$outcome->value.' '.$message->getRoutingKey());
    }

    /** Creates exchanges and queues if they do not exist yet. Declaring them again with the same settings is harmless. */
    private function declareTopology(AMQPChannel $channel): void
    {
        // exchange_declare(name, type, passive, durable, auto_delete)
        $channel->exchange_declare(self::EXCHANGE, 'topic', false, true, false);
        $channel->exchange_declare(self::DEAD_LETTER_EXCHANGE, 'direct', false, true, false);

        // queue_declare(name, passive, durable, exclusive, auto_delete, nowait, arguments)
        $channel->queue_declare(self::DEAD_LETTER_QUEUE, false, true, false, false);
        $channel->queue_bind(self::DEAD_LETTER_QUEUE, self::DEAD_LETTER_EXCHANGE, self::DEAD_LETTER_ROUTING_KEY);

        $channel->queue_declare(self::QUEUE, false, true, false, false, false, new AMQPTable([
            'x-dead-letter-exchange' => self::DEAD_LETTER_EXCHANGE,
            'x-dead-letter-routing-key' => self::DEAD_LETTER_ROUTING_KEY,
        ]));
        $channel->queue_bind(self::QUEUE, self::EXCHANGE, 'return.status.#');
    }
}
