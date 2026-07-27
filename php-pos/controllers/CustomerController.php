<?php
declare(strict_types=1);

class CustomerController {
    public function index(array $params, array $body): array {
        $search = $params['search'] ?? '';
        
        $where = ['deleted_at IS NULL'];
        $queryParams = [];
        
        if (!empty($search)) {
            $where[] = '(name LIKE ? OR phone LIKE ? OR email LIKE ?)';
            $searchTerm = "%$search%";
            $queryParams[] = $searchTerm;
            $queryParams[] = $searchTerm;
            $queryParams[] = $searchTerm;
        }
        
        $whereClause = implode(' AND ', $where);
        
        $customers = Database::fetchAll(
            "SELECT * FROM customers WHERE $whereClause ORDER BY name",
            $queryParams
        );
        
        return ['data' => $customers];
    }

    public function store(array $params, array $body): array {
        if (empty($body['name'])) {
            http_response_code(400);
            return ['error' => 'Customer name is required'];
        }
        
        $id = bin2hex(random_bytes(16));
        
        Database::execute(
            'INSERT INTO customers (id, name, phone, email, loyalty_points, credit_balance, created_at, updated_at) 
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)',
            [
                $id,
                $body['name'],
                $body['phone'] ?? null,
                $body['email'] ?? null,
                (int)($body['loyalty_points'] ?? 0),
                (float)($body['credit_balance'] ?? 0),
                date('Y-m-d H:i:s'),
                date('Y-m-d H:i:s'),
            ]
        );
        
        http_response_code(201);
        return ['id' => $id, 'message' => 'Customer created successfully'];
    }

    public function show(array $params, array $body): array {
        $customer = Database::fetchOne(
            'SELECT * FROM customers WHERE id = ? AND deleted_at IS NULL',
            [$params['id']]
        );
        
        if (!$customer) {
            http_response_code(404);
            return ['error' => 'Customer not found'];
        }
        
        return $customer;
    }

    public function update(array $params, array $body): array {
        $customer = Database::fetchOne(
            'SELECT * FROM customers WHERE id = ? AND deleted_at IS NULL',
            [$params['id']]
        );
        
        if (!$customer) {
            http_response_code(404);
            return ['error' => 'Customer not found'];
        }
        
        Database::execute(
            'UPDATE customers SET name = ?, phone = ?, email = ?, loyalty_points = ?, credit_balance = ?, updated_at = ? WHERE id = ?',
            [
                $body['name'] ?? $customer['name'],
                $body['phone'] ?? $customer['phone'],
                $body['email'] ?? $customer['email'],
                (int)($body['loyalty_points'] ?? $customer['loyalty_points']),
                (float)($body['credit_balance'] ?? $customer['credit_balance']),
                date('Y-m-d H:i:s'),
                $params['id'],
            ]
        );
        
        return ['message' => 'Customer updated successfully'];
    }

    public function destroy(array $params, array $body): array {
        $customer = Database::fetchOne(
            'SELECT * FROM customers WHERE id = ? AND deleted_at IS NULL',
            [$params['id']]
        );
        
        if (!$customer) {
            http_response_code(404);
            return ['error' => 'Customer not found'];
        }
        
        Database::execute(
            'UPDATE customers SET deleted_at = ? WHERE id = ?',
            [date('Y-m-d H:i:s'), $params['id']]
        );
        
        return ['message' => 'Customer deleted successfully'];
    }
}
