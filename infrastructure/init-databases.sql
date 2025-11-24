-- Create databases
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE inventory_db;
CREATE DATABASE saga_db;
CREATE DATABASE notification_db;

-- Connect to order_db
\c order_db;

CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        user_id VARCHAR(255) NOT NULL,
                        total_amount DECIMAL(19, 2) NOT NULL,
                        status VARCHAR(50) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
                             id BIGSERIAL PRIMARY KEY,
                             order_id BIGINT NOT NULL,
                             product_id VARCHAR(255) NOT NULL,
                             quantity INT NOT NULL,
                             price DECIMAL(19, 2) NOT NULL,
                             FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);

-- Connect to payment_db
\c payment_db;

CREATE TABLE payments (
                          id BIGSERIAL PRIMARY KEY,
                          order_id BIGINT NOT NULL,
                          amount DECIMAL(19, 2) NOT NULL,
                          status VARCHAR(50) NOT NULL,
                          payment_method VARCHAR(50),
                          transaction_id VARCHAR(255) UNIQUE,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);

-- Connect to inventory_db
\c inventory_db;

CREATE TABLE inventory (
                           id BIGSERIAL PRIMARY KEY,
                           product_id VARCHAR(255) NOT NULL UNIQUE,
                           product_name VARCHAR(255) NOT NULL,
                           available_stock INT NOT NULL DEFAULT 0,
                           reserved_stock INT NOT NULL DEFAULT 0,
                           version INT NOT NULL DEFAULT 0,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           CONSTRAINT check_stock_non_negative CHECK (available_stock >= 0),
                           CONSTRAINT check_reserved_non_negative CHECK (reserved_stock >= 0)
);

CREATE TABLE processed_commands (
                                    id BIGSERIAL PRIMARY KEY,
                                    command_id VARCHAR(255) NOT NULL UNIQUE,
                                    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_inventory_product_id ON inventory(product_id);
CREATE INDEX idx_processed_commands_command_id ON processed_commands(command_id);

-- Insert sample inventory data
INSERT INTO inventory (product_id, product_name, available_stock, reserved_stock) VALUES
                                                                                      ('PROD-001', 'Laptop Dell XPS 15', 50, 0),
                                                                                      ('PROD-002', 'iPhone 15 Pro', 100, 0),
                                                                                      ('PROD-003', 'Sony WH-1000XM5 Headphones', 75, 0),
                                                                                      ('PROD-004', 'Samsung Galaxy S24', 80, 0),
                                                                                      ('PROD-005', 'MacBook Pro M3', 30, 0);

-- Connect to saga_db
\c saga_db;

CREATE TABLE saga_instance (
                               id BIGSERIAL PRIMARY KEY,
                               saga_id VARCHAR(255) NOT NULL UNIQUE,
                               order_id BIGINT NOT NULL,
                               status VARCHAR(50) NOT NULL,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE saga_step (
                           id BIGSERIAL PRIMARY KEY,
                           saga_id VARCHAR(255) NOT NULL,
                           step_name VARCHAR(100) NOT NULL,
                           status VARCHAR(50) NOT NULL,
                           step_data TEXT,
                           error_message TEXT,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           FOREIGN KEY (saga_id) REFERENCES saga_instance(saga_id) ON DELETE CASCADE
);

CREATE INDEX idx_saga_instance_saga_id ON saga_instance(saga_id);
CREATE INDEX idx_saga_instance_order_id ON saga_instance(order_id);
CREATE INDEX idx_saga_step_saga_id ON saga_step(saga_id);

-- Connect to notification_db
\c notification_db;

CREATE TABLE notifications (
                               id BIGSERIAL PRIMARY KEY,
                               user_id VARCHAR(255) NOT NULL,
                               order_id BIGINT,
                               message TEXT NOT NULL,
                               type VARCHAR(50) NOT NULL,
                               is_read BOOLEAN DEFAULT FALSE,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_order_id ON notifications(order_id);