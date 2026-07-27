<?php
declare(strict_types=1);

class CategoryController {
    public function index(array $params, array $body): array {
        $categories = Database::fetchAll(
            'SELECT * FROM categories WHERE deleted_at IS NULL ORDER BY name'
        );
        return ['data' => $categories];
    }

    public function store(array $params, array $body): array {
        if (empty($body['name'])) {
            http_response_code(400);
            return ['error' => 'Category name is required'];
        }
        
        $id = bin2hex(random_bytes(16));
        
        Database::execute(
            'INSERT INTO categories (id, name, description, created_at, updated_at) VALUES (?, ?, ?, ?, ?)',
            [
                $id,
                $body['name'],
                $body['description'] ?? null,
                date('Y-m-d H:i:s'),
                date('Y-m-d H:i:s'),
            ]
        );
        
        http_response_code(201);
        return ['id' => $id, 'message' => 'Category created successfully'];
    }

    public function update(array $params, array $body): array {
        $category = Database::fetchOne(
            'SELECT * FROM categories WHERE id = ? AND deleted_at IS NULL',
            [$params['id']]
        );
        
        if (!$category) {
            http_response_code(404);
            return ['error' => 'Category not found'];
        }
        
        Database::execute(
            'UPDATE categories SET name = ?, description = ?, updated_at = ? WHERE id = ?',
            [
                $body['name'] ?? $category['name'],
                $body['description'] ?? $category['description'],
                date('Y-m-d H:i:s'),
                $params['id'],
            ]
        );
        
        return ['message' => 'Category updated successfully'];
    }

    public function destroy(array $params, array $body): array {
        $category = Database::fetchOne(
            'SELECT * FROM categories WHERE id = ? AND deleted_at IS NULL',
            [$params['id']]
        );
        
        if (!$category) {
            http_response_code(404);
            return ['error' => 'Category not found'];
        }
        
        Database::execute(
            'UPDATE categories SET deleted_at = ? WHERE id = ?',
            [date('Y-m-d H:i:s'), $params['id']]
        );
        
        return ['message' => 'Category deleted successfully'];
    }
}
