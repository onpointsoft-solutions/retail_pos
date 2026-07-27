<?php
declare(strict_types=1);

class User {
    private string $id;
    private string $username;
    private string $passwordHash;
    private string $role;
    private ?string $fullName;
    private bool $active;
    private int $failedLoginAttempts;
    private ?DateTime $lockoutUntil;
    private DateTime $createdAt;
    private DateTime $updatedAt;

    public function __construct(array $data = []) {
        $this->id = $data['id'] ?? $this->generateId();
        $this->username = $data['username'] ?? '';
        $this->passwordHash = $data['password_hash'] ?? '';
        $this->role = $data['role'] ?? 'CASHIER';
        $this->fullName = $data['full_name'] ?? null;
        $this->active = (bool)($data['active'] ?? true);
        $this->failedLoginAttempts = (int)($data['failed_login_attempts'] ?? 0);
        $this->lockoutUntil = isset($data['lockout_until']) && $data['lockout_until'] 
            ? new DateTime($data['lockout_until']) 
            : null;
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
            'username' => $this->username,
            'password_hash' => $this->passwordHash,
            'role' => $this->role,
            'full_name' => $this->fullName,
            'active' => $this->active ? 1 : 0,
            'failed_login_attempts' => $this->failedLoginAttempts,
            'lockout_until' => $this->lockoutUntil?->format('Y-m-d H:i:s'),
            'created_at' => $this->createdAt->format('Y-m-d H:i:s'),
            'updated_at' => $this->updatedAt->format('Y-m-d H:i:s'),
        ];
    }

    public function toPublicArray(): array {
        return [
            'id' => $this->id,
            'username' => $this->username,
            'role' => $this->role,
            'full_name' => $this->fullName,
            'active' => $this->active,
            'created_at' => $this->createdAt->format('Y-m-d H:i:s'),
        ];
    }

    // Getters
    public function getId(): string { return $this->id; }
    public function getUsername(): string { return $this->username; }
    public function getPasswordHash(): string { return $this->passwordHash; }
    public function getRole(): string { return $this->role; }
    public function getFullName(): ?string { return $this->fullName; }
    public function isActive(): bool { return $this->active; }
    public function getFailedLoginAttempts(): int { return $this->failedLoginAttempts; }
    public function getLockoutUntil(): ?DateTime { return $this->lockoutUntil; }

    // Setters
    public function setUsername(string $username): void { $this->username = $username; }
    public function setPasswordHash(string $hash): void { $this->passwordHash = $hash; }
    public function setRole(string $role): void { $this->role = $role; }
    public function setFullName(?string $name): void { $this->fullName = $name; }
    public function setActive(bool $active): void { $this->active = $active; }
    public function setFailedLoginAttempts(int $attempts): void { $this->failedLoginAttempts = $attempts; }
    public function setLockoutUntil(?DateTime $date): void { $this->lockoutUntil = $date; }
    public function setUpdatedAt(DateTime $date): void { $this->updatedAt = $date; }

    public function isLocked(): bool {
        return $this->lockoutUntil !== null && $this->lockoutUntil > new DateTime();
    }

    public function isAdmin(): bool {
        return $this->role === 'ADMIN';
    }

    public function canManageProducts(): bool {
        return in_array($this->role, ['ADMIN', 'MANAGER']);
    }

    public function canManageUsers(): bool {
        return $this->role === 'ADMIN';
    }
}
