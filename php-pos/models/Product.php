<?php
declare(strict_types=1);

class Product {
    private string $id;
    private ?string $barcode;
    private ?string $qrCode;
    private string $sku;
    private string $name;
    private ?string $categoryId;
    private float $buyingPrice;
    private float $sellingPrice;
    private float $wholesalePrice;
    private int $currentStock;
    private int $minimumStock;
    private int $preferredOrderQuantity;
    private float $taxRate;
    private float $discount;
    private ?string $supplierId;
    private ?string $description;
    private ?string $imagePath;
    private string $unit;
    private string $status;
    private bool $trackExpiry;
    private int $version;
    private DateTime $createdAt;
    private DateTime $updatedAt;

    public function __construct(array $data = []) {
        $this->id = $data['id'] ?? $this->generateId();
        $this->barcode = $data['barcode'] ?? null;
        $this->qrCode = $data['qr_code'] ?? null;
        $this->sku = $data['sku'] ?? '';
        $this->name = $data['name'] ?? '';
        $this->categoryId = $data['category_id'] ?? null;
        $this->buyingPrice = (float)($data['buying_price'] ?? 0);
        $this->sellingPrice = (float)($data['selling_price'] ?? 0);
        $this->wholesalePrice = (float)($data['wholesale_price'] ?? 0);
        $this->currentStock = (int)($data['current_stock'] ?? 0);
        $this->minimumStock = (int)($data['minimum_stock'] ?? 0);
        $this->preferredOrderQuantity = (int)($data['preferred_order_quantity'] ?? 0);
        $this->taxRate = (float)($data['tax_rate'] ?? 0);
        $this->discount = (float)($data['discount'] ?? 0);
        $this->supplierId = $data['supplier_id'] ?? null;
        $this->description = $data['description'] ?? null;
        $this->imagePath = $data['image_path'] ?? null;
        $this->unit = $data['unit'] ?? 'pcs';
        $this->status = $data['status'] ?? 'active';
        $this->trackExpiry = (bool)($data['track_expiry'] ?? false);
        $this->version = (int)($data['version'] ?? 1);
        $this->createdAt = isset($data['created_at']) 
            ? new DateTime($data['created_at']) 
            : new DateTime();
        $this->updatedAt = isset($data['updated_at']) 
            ? new DateTime($data['updated_at']) 
            : new DateTime();
    }

    private function generateId(): string {
        return bin2hex(random_bytes(16));
    }

    public function toArray(): array {
        return [
            'id' => $this->id,
            'barcode' => $this->barcode,
            'qr_code' => $this->qrCode,
            'sku' => $this->sku,
            'name' => $this->name,
            'category_id' => $this->categoryId,
            'buying_price' => $this->buyingPrice,
            'selling_price' => $this->sellingPrice,
            'wholesale_price' => $this->wholesalePrice,
            'current_stock' => $this->currentStock,
            'minimum_stock' => $this->minimumStock,
            'preferred_order_quantity' => $this->preferredOrderQuantity,
            'tax_rate' => $this->taxRate,
            'discount' => $this->discount,
            'supplier_id' => $this->supplierId,
            'description' => $this->description,
            'image_path' => $this->imagePath,
            'unit' => $this->unit,
            'status' => $this->status,
            'track_expiry' => $this->trackExpiry ? 1 : 0,
            'version' => $this->version,
            'created_at' => $this->createdAt->format('Y-m-d H:i:s'),
            'updated_at' => $this->updatedAt->format('Y-m-d H:i:s'),
        ];
    }

    // Getters
    public function getId(): string { return $this->id; }
    public function getBarcode(): ?string { return $this->barcode; }
    public function getSku(): string { return $this->sku; }
    public function getName(): string { return $this->name; }
    public function getSellingPrice(): float { return $this->sellingPrice; }
    public function getCurrentStock(): int { return $this->currentStock; }
    public function getMinimumStock(): int { return $this->minimumStock; }
    public function getStatus(): string { return $this->status; }
    public function getImagePath(): ?string { return $this->imagePath; }

    // Setters
    public function setBarcode(?string $barcode): void { $this->barcode = $barcode; }
    public function setSku(string $sku): void { $this->sku = $sku; }
    public function setName(string $name): void { $this->name = $name; }
    public function setSellingPrice(float $price): void { $this->sellingPrice = $price; }
    public function setCurrentStock(int $stock): void { $this->currentStock = $stock; }
    public function setMinimumStock(int $stock): void { $this->minimumStock = $stock; }
    public function setStatus(string $status): void { $this->status = $status; }
    public function setImagePath(?string $path): void { $this->imagePath = $path; }
    public function setUpdatedAt(DateTime $date): void { $this->updatedAt = $date; }

    public function isLowStock(): bool {
        return $this->currentStock <= $this->minimumStock;
    }

    public function isActive(): bool {
        return $this->status === 'active';
    }
}
