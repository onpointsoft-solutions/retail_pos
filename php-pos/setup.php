<?php
declare(strict_types=1);

session_start();

$error = '';
$success = false;

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $step = $_POST['step'] ?? 1;
    
    if ($step == 1) {
        // Database configuration
        $_SESSION['db_host'] = $_POST['db_host'] ?? 'localhost';
        $_SESSION['db_name'] = $_POST['db_name'] ?? 'retail_pos';
        $_SESSION['db_user'] = $_POST['db_user'] ?? 'root';
        $_SESSION['db_pass'] = $_POST['db_pass'] ?? '';
        
        // Test database connection
        try {
            $dsn = "mysql:host={$_SESSION['db_host']};dbname={$_SESSION['db_name']};charset=utf8mb4";
            $pdo = new PDO($dsn, $_SESSION['db_user'], $_SESSION['db_pass']);
            $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            $_SESSION['step'] = 2;
        } catch (PDOException $e) {
            $error = 'Database connection failed: ' . $e->getMessage();
        }
    } elseif ($step == 2) {
        // Store settings
        $_SESSION['store_name'] = $_POST['store_name'] ?? '';
        $_SESSION['store_address'] = $_POST['store_address'] ?? '';
        $_SESSION['store_phone'] = $_POST['store_phone'] ?? '';
        $_SESSION['tax_rate'] = $_POST['tax_rate'] ?? '16';
        $_SESSION['step'] = 3;
    } elseif ($step == 3) {
        // Admin user
        $username = $_POST['admin_username'] ?? 'admin';
        $password = $_POST['admin_password'] ?? '';
        $fullName = $_POST['admin_fullname'] ?? 'Administrator';
        
        if (strlen($password) < 6) {
            $error = 'Password must be at least 6 characters';
        } else {
            // Import schema
            $schema = file_get_contents(__DIR__ . '/database/schema.sql');
            $dsn = "mysql:host={$_SESSION['db_host']};dbname={$_SESSION['db_name']};charset=utf8mb4";
            $pdo = new PDO($dsn, $_SESSION['db_user'], $_SESSION['db_pass']);
            $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            
            try {
                $pdo->exec($schema);
                
                // Create admin user
                $passwordHash = password_hash($password, PASSWORD_BCRYPT);
                $adminId = bin2hex(random_bytes(16));
                
                $stmt = $pdo->prepare(
                    'INSERT INTO users (id, username, password_hash, role, full_name, active, created_at, updated_at) 
                     VALUES (?, ?, ?, ?, ?, 1, NOW(), NOW())'
                );
                $stmt->execute([$adminId, $username, $passwordHash, 'ADMIN', $fullName]);
                
                // Insert default settings
                $settings = [
                    'store_name' => $_SESSION['store_name'],
                    'store_address' => $_SESSION['store_address'],
                    'store_phone' => $_SESSION['store_phone'],
                    'tax_rate' => $_SESSION['tax_rate'],
                    'setup_complete' => 'true',
                    'primary_color' => '#2980b9',
                    'secondary_color' => '#3498db',
                    'accent_color' => '#e74c3c',
                    'success_color' => '#27ae60',
                    'warning_color' => '#f39c12',
                ];
                
                foreach ($settings as $key => $value) {
                    $stmt = $pdo->prepare('INSERT INTO app_settings (`key`, `value`, updated_at) VALUES (?, ?, NOW())');
                    $stmt->execute([$key, $value]);
                }
                
                // Insert default categories
                $categories = ['General', 'Food & Beverages', 'Electronics', 'Clothing', 'Household'];
                foreach ($categories as $cat) {
                    $catId = bin2hex(random_bytes(16));
                    $stmt = $pdo->prepare('INSERT INTO categories (id, name, created_at, updated_at) VALUES (?, ?, NOW(), NOW())');
                    $stmt->execute([$catId, $cat]);
                }
                
                $success = true;
                session_destroy();
            } catch (PDOException $e) {
                $error = 'Setup failed: ' . $e->getMessage();
            }
        }
    }
}

$step = $_SESSION['step'] ?? 1;
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Retail POS - Setup</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f5f5f5; }
        .container { max-width: 600px; margin: 50px auto; background: white; padding: 40px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        h1 { color: #2980b9; margin-bottom: 10px; }
        .step-indicator { color: #666; margin-bottom: 30px; }
        .form-group { margin-bottom: 20px; }
        label { display: block; margin-bottom: 8px; font-weight: 600; color: #333; }
        input { width: 100%; padding: 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; }
        input:focus { outline: none; border-color: #2980b9; }
        button { background: #2980b9; color: white; padding: 12px 24px; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 600; }
        button:hover { background: #3498db; }
        .error { background: #fee; color: #c33; padding: 12px; border-radius: 6px; margin-bottom: 20px; }
        .success { background: #efe; color: #3c3; padding: 20px; border-radius: 6px; text-align: center; }
        .success a { display: inline-block; margin-top: 15px; background: #27ae60; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; }
    </style>
</head>
<body>
    <div class="container">
        <?php if ($success): ?>
            <h1>Setup Complete!</h1>
            <div class="success">
                <p>Your Retail POS system has been configured successfully.</p>
                <a href="index.php">Go to Login</a>
            </div>
        <?php else: ?>
            <h1>Retail POS Setup</h1>
            <p class="step-indicator">Step <?php echo $step; ?> of 3</p>
            
            <?php if ($error): ?>
                <div class="error"><?php echo htmlspecialchars($error); ?></div>
            <?php endif; ?>
            
            <form method="POST">
                <input type="hidden" name="step" value="<?php echo $step; ?>">
                
                <?php if ($step == 1): ?>
                    <h2>Database Configuration</h2>
                    <div class="form-group">
                        <label>Database Host</label>
                        <input type="text" name="db_host" value="localhost" required>
                    </div>
                    <div class="form-group">
                        <label>Database Name</label>
                        <input type="text" name="db_name" value="retail_pos" required>
                    </div>
                    <div class="form-group">
                        <label>Database Username</label>
                        <input type="text" name="db_user" value="root" required>
                    </div>
                    <div class="form-group">
                        <label>Database Password</label>
                        <input type="password" name="db_pass">
                    </div>
                    <button type="submit">Continue</button>
                    
                <?php elseif ($step == 2): ?>
                    <h2>Store Information</h2>
                    <div class="form-group">
                        <label>Store Name *</label>
                        <input type="text" name="store_name" required>
                    </div>
                    <div class="form-group">
                        <label>Store Address</label>
                        <input type="text" name="store_address">
                    </div>
                    <div class="form-group">
                        <label>Store Phone</label>
                        <input type="text" name="store_phone">
                    </div>
                    <div class="form-group">
                        <label>Tax Rate (%)</label>
                        <input type="number" name="tax_rate" value="16" step="0.1">
                    </div>
                    <button type="submit">Continue</button>
                    
                <?php elseif ($step == 3): ?>
                    <h2>Admin Account</h2>
                    <div class="form-group">
                        <label>Username</label>
                        <input type="text" name="admin_username" value="admin" required>
                    </div>
                    <div class="form-group">
                        <label>Full Name</label>
                        <input type="text" name="admin_fullname" value="Administrator">
                    </div>
                    <div class="form-group">
                        <label>Password *</label>
                        <input type="password" name="admin_password" required>
                    </div>
                    <button type="submit">Complete Setup</button>
                <?php endif; ?>
            </form>
        <?php endif; ?>
    </div>
</body>
</html>
