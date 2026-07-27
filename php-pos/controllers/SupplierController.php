<?php
declare(strict_types=1);

class SupplierController {
    public function index(array $params, array $body): array {
        $suppliers = Database::fetchAll(
            'SELECT * FROM suppliers WHERE deleted_at IS NULL ORDER BY name'
        );
        return ['data' => $suppliers];
    }

    public function store(array $params, array $body): array {
        if (empty($body['name'])) {
            http_response_code(400);
            return ['error' => 'Supplier name is required'];
        }
        
        $id = bin2hex(random_bytes(16));
        
        Database::execute(
            'INSERT INTO suppliers (id, name, phone, email, address, balance, created_at, updated_at) 
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)',
            [
                $id,
                $body['name'],
                $body['phone'] ?? null,
                $body['email'] ?? null,
                $body['address'] ?? null,
                (float)($body['balance'] ?? 0),
                date('Y-m-d H:i:s'),
                date('Y-m-d H:i:s'),
            ]
        );
        
        http_response_code(201);
        return ['id' => $id, 'message' => 'Supplier created successfully'];
    }

    public function update(array $params, array $body): array {
        $supplier = Database::fetchOne(
            'SELECT * FROM suppliers WHERE id = ? AND deleted_at IS NULL',
            [$params['id']]
        );
        
        if (!$supplier) {
            http_response_code(404);
            return ['error' => 'Supplier not found'];
        }
        
        Database::execute(
            'UPDATE suppliers SET name = ?, phone = ?, email = ?, address = ?, balance = ?, updated_at = ? WHERE id = ?',
            [
                $body['name'] ?? $supplier['name'],
                $body['phone'] ?? $supplier['phone'],
                $body['email'] ?? $supplier['email'],
                $body['address'] ?? $supplier['address'],
                (float)($body['balance'] ?? $supplier['balance']),
                date('Y-m-d H:i:s'),
                $params['id'],
            ]
        );
        
        return ['message' => 'Supplier updated successfully'];
    }

    public function destroy(array $params, array $body): array {
        $supplier = Database::fetchOne(
            'SELECT * FROM suppliers WHERE id = ? AND deleted_at IS NULL',
            [$params['id']]
        );
        
        if (!$supplier) {
            http_response_code(404);
            return ['error' => 'Supplier not found'];
        }
        
        Database::execute(
            'UPDATE suppliers SET deleted_at = ? WHERE id = ?',
            [date('Y-m-d H:i:s'), $params['id']]
        );
        
        return ['message' => 'Supplier deleted successfully'];
    }
}
