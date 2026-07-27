<?php
declare(strict_types=1);

class ProductController {
    public function index(array $params, array $body): array {
        $page = (int)($params['page'] ?? 1);
        $limit = (int)($params['limit'] ?? DEFAULT_PAGE_SIZE);
        $offset = ($page - 1) * $limit;
        
        $search = $params['search'] ?? '';
        $category = $params['category'] ?? '';
        $status = $params['status'] ?? '';
        
        $where = ['deleted_at IS NULL'];
        $queryParams = [];
        
        if (!empty($search)) {
            $where[] = '(name LIKE ? OR barcode LIKE ? OR sku LIKE ?)';
            $searchTerm = "%$search%";
            $queryParams[] = $searchTerm;
            $queryParams[] = $searchTerm;
            $queryParams[] = $searchTerm;
        }
        
        if (!empty($category)) {
            $where[] = 'category_id = ?';
            $queryParams[] = $category;
        }
        
        if (!empty($status)) {
            $where[] = 'status = ?';
            $queryParams[] = $status;
        }
        
        $whereClause = implode(' AND ', $where);
        
        $products = Database::fetchAll(
            "SELECT * FROM products WHERE $whereClause ORDER BY name LIMIT ? OFFSET ?",
            [...$queryParams, $limit, $offset]
        );
        
        $total = Database::fetchOne(
            "SELECT COUNT(*) as count FROM products WHERE $whereClause",
            $queryParams
        )['count'] ?? 0;
        
        return [
            'data' => array_map(fn($p) => new Product($p), $products),
            'meta' => [
                'total' => (int)$total,
                'page' => $page,
                'limit' => $limit,
                'pages' => ceil($total / $limit),
            ],
        ];
    }

    public function store(array $params, array $body): array {
        $product = new Product($body);
        
        // Validate required fields
        if (empty($product->getSku()) || empty($product->getName())) {
            http_response_code(400);
            return ['error' => 'SKU and name are required'];
        }
        
        // Check if SKU already exists
        $existing = Database::fetchOne(
            'SELECT id FROM products WHERE sku = ? AND deleted_at IS NULL',
            [$product->getSku()]
        );
        
        if ($existing) {
            http_response_code(409);
            return ['error' => 'SKU already exists'];
        }
        
        Database::execute(
            'INSERT INTO products (id, barcode, qr_code, sku, name, category_id, buying_price, selling_price, 
             wholesale_price, current_stock, minimum_stock, preferred_order_quantity, tax_rate, discount, 
             supplier_id, description, image_path, unit, status, track_expiry, version, created_at, updated_at) 
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)',
            $product->toArray()
        );
        
        http_response_code(201);
        return $product->toArray();
    }

    public function show(array $params, array $body): array {
        $productData = Database::fetchOne(
            'SELECT * FROM products WHERE id = ? AND deleted_at IS NULL',
            [$params['id']]
        );
        
        if (!$productData) {
            http_response_code(404);
            return ['error' => 'Product not found'];
        }
        
        return new Product($productData);
    }

    public function update(array $params, array $body): array {
        $productData = Database::fetchOne(
            'SELECT * FROM products WHERE id = ? AND deleted_at IS NULL',
            [$params['id']]
        );
        
        if (!$productData) {
            http_response_code(404);
            return ['error' => 'Product not found'];
        }
        
        $product = new Product($productData);
        
        // Update fields from body
        if (isset($body['barcode'])) $product->setBarcode($body['barcode']);
        if (isset($body['sku'])) $product->setSku($body['sku']);
        if (isset($body['name'])) $product->setName($body['name']);
        if (isset($body['selling_price'])) $product->setSellingPrice((float)$body['selling_price']);
        if (isset($body['current_stock'])) $product->setCurrentStock((int)$body['current_stock']);
        if (isset($body['minimum_stock'])) $product->setMinimumStock((int)$body['minimum_stock']);
        if (isset($body['status'])) $product->setStatus($body['status']);
        if (isset($body['image_path'])) $product->setImagePath($body['image_path']);
        
        $product->setUpdatedAt(new DateTime());
        
        Database::execute(
            'UPDATE products SET barcode = ?, sku = ?, name = ?, selling_price = ?, current_stock = ?, 
             minimum_stock = ?, status = ?, image_path = ?, updated_at = ? WHERE id = ?',
            [
                $product->getBarcode(),
                $product->getSku(),
                $product->getName(),
                $product->getSellingPrice(),
                $product->getCurrentStock(),
                $product->getMinimumStock(),
                $product->getStatus(),
                $product->getImagePath(),
                $product->getUpdatedAt()->format('Y-m-d H:i:s'),
                $product->getId(),
            ]
        );
        
        return $product->toArray();
    }

    public function destroy(array $params, array $body): array {
        $productData = Database::fetchOne(
            'SELECT * FROM products WHERE id = ? AND deleted_at IS NULL',
            [$params['id']]
        );
        
        if (!$productData) {
            http_response_code(404);
            return ['error' => 'Product not found'];
        }
        
        Database::execute(
            'UPDATE products SET deleted_at = ? WHERE id = ?',
            [date('Y-m-d H:i:s'), $params['id']]
        );
        
        return ['message' => 'Product deleted successfully'];
    }
}
