-- Ensure the 'customers' table exists
CREATE TABLE IF NOT EXISTS customers
(
    id UUID PRIMARY KEY,
    external_user_id UUID NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    phone_number VARCHAR(50),
    kyc_status VARCHAR(20) NOT NULL,
    date_of_birth DATE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
    );

-- Insert well-known UUIDs for specific customers
INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174000',
       '223e4567-e89b-12d3-a456-426614174000',
       'John',
       'Doe',
       'john.doe@example.com',
       '123-456-7890',
       'VERIFIED',
       '1990-05-15',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1
                  FROM customers
                  WHERE id = '123e4567-e89b-12d3-a456-426614174000');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174001',
       '223e4567-e89b-12d3-a456-426614174001',
       'Jane',
       'Smith',
       'jane.smith@example.com',
       '987-654-3210',
       'PENDING',
       '1985-09-30',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1
                  FROM customers
                  WHERE id = '123e4567-e89b-12d3-a456-426614174001');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174002',
       '223e4567-e89b-12d3-a456-426614174002',
       'Alice',
       'Johnson',
       'alice.johnson@example.com',
       '555-555-5555',
       'REJECTED',
       '1992-12-01',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1
                  FROM customers
                  WHERE id = '123e4567-e89b-12d3-a456-426614174002');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174003',
       '223e4567-e89b-12d3-a456-426614174003',
       'Bob',
       'Brown',
       'bob.brown@example.com',
       '321-654-9870',
       'VERIFIED',
       '1982-11-30',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1
                  FROM customers
                  WHERE id = '123e4567-e89b-12d3-a456-426614174003');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174004',
       '223e4567-e89b-12d3-a456-426614174004',
       'Emily',
       'Davis',
       'emily.davis@example.com',
       '654-321-0987',
       'PENDING',
       '1995-02-05',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1
                  FROM customers
                  WHERE id = '123e4567-e89b-12d3-a456-426614174004');

-- Insert 20 dummy customers into 'customers' table
INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174005',
       '223e4567-e89b-12d3-a456-426614174005',
       'Michael',
       'Green',
       'michael.green@example.com',
       '987-123-4567',
       'VERIFIED',
       '1988-07-25',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM customers WHERE id = '123e4567-e89b-12d3-a456-426614174005');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174006',
       '223e4567-e89b-12d3-a456-426614174006',
       'Sarah',
       'Taylor',
       'sarah.taylor@example.com',
       '123-987-6543',
       'PENDING',
       '1992-04-18',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM customers WHERE id = '123e4567-e89b-12d3-a456-426614174006');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174007',
       '223e4567-e89b-12d3-a456-426614174007',
       'David',
       'Wilson',
       'david.wilson@example.com',
       '555-678-1234',
       'REJECTED',
       '1975-01-11',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM customers WHERE id = '123e4567-e89b-12d3-a456-426614174007');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174008',
       '223e4567-e89b-12d3-a456-426614174008',
       'Laura',
       'White',
       'laura.white@example.com',
       '456-321-9870',
       'VERIFIED',
       '1989-09-02',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM customers WHERE id = '123e4567-e89b-12d3-a456-426614174008');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174009',
       '223e4567-e89b-12d3-a456-426614174009',
       'James',
       'Harris',
       'james.harris@example.com',
       '321-555-6789',
       'PENDING',
       '1993-11-15',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM customers WHERE id = '123e4567-e89b-12d3-a456-426614174009');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174010',
       '223e4567-e89b-12d3-a456-426614174010',
       'Emma',
       'Moore',
       'emma.moore@example.com',
       '789-123-4560',
       'VERIFIED',
       '1980-08-09',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM customers WHERE id = '123e4567-e89b-12d3-a456-426614174010');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174011',
       '223e4567-e89b-12d3-a456-426614174011',
       'Ethan',
       'Martinez',
       'ethan.martinez@example.com',
       '987-654-3211',
       'REJECTED',
       '1984-05-03',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM customers WHERE id = '123e4567-e89b-12d3-a456-426614174011');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174012',
       '223e4567-e89b-12d3-a456-426614174012',
       'Sophia',
       'Clark',
       'sophia.clark@example.com',
       '555-987-6540',
       'VERIFIED',
       '1991-12-25',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM customers WHERE id = '123e4567-e89b-12d3-a456-426614174012');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174013',
       '223e4567-e89b-12d3-a456-426614174013',
       'Daniel',
       'Lewis',
       'daniel.lewis@example.com',
       '321-789-4560',
       'PENDING',
       '1976-06-08',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM customers WHERE id = '123e4567-e89b-12d3-a456-426614174013');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174014',
       '223e4567-e89b-12d3-a456-426614174014',
       'Isabella',
       'Walker',
       'isabella.walker@example.com',
       '654-123-7890',
       'REJECTED',
       '1987-10-17',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM customers WHERE id = '123e4567-e89b-12d3-a456-426614174014');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174015',
       '223e4567-e89b-12d3-a456-426614174015',
       'Liam',
       'Scott',
       'liam.scott@example.com',
       '789-456-1230',
       'VERIFIED',
       '1983-03-22',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM customers WHERE id = '123e4567-e89b-12d3-a456-426614174015');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174016',
       '223e4567-e89b-12d3-a456-426614174016',
       'Olivia',
       'Adams',
       'olivia.adams@example.com',
       '456-789-1230',
       'PENDING',
       '1994-07-11',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM customers WHERE id = '123e4567-e89b-12d3-a456-426614174016');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174017',
       '223e4567-e89b-12d3-a456-426614174017',
       'Noah',
       'Baker',
       'noah.baker@example.com',
       '321-654-9871',
       'VERIFIED',
       '1986-09-05',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM customers WHERE id = '123e4567-e89b-12d3-a456-426614174017');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174018',
       '223e4567-e89b-12d3-a456-426614174018',
       'Ava',
       'Carter',
       'ava.carter@example.com',
       '555-321-9870',
       'REJECTED',
       '1990-11-19',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM customers WHERE id = '123e4567-e89b-12d3-a456-426614174018');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174019',
       '223e4567-e89b-12d3-a456-426614174019',
       'William',
       'Evans',
       'william.evans@example.com',
       '789-321-6540',
       'VERIFIED',
       '1982-12-12',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM customers WHERE id = '123e4567-e89b-12d3-a456-426614174019');

INSERT INTO customers (id, external_user_id, first_name, last_name, email, phone_number, kyc_status, date_of_birth, created_at, updated_at)
SELECT '123e4567-e89b-12d3-a456-426614174020',
       '223e4567-e89b-12d3-a456-426614174020',
       'Mia',
       'Hall',
       'mia.hall@example.com',
       '123-456-7891',
       'PENDING',
       '1993-05-27',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM customers WHERE id = '123e4567-e89b-12d3-a456-426614174020');
