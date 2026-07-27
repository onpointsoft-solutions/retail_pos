<?php
declare(strict_types=1);

session_start();

require_once __DIR__ . '/config/database.php';

$error = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = $_POST['username'] ?? '';
    $password = $_POST['password'] ?? '';
    
    $userData = Database::fetchOne(
        'SELECT * FROM users WHERE username = ? AND deleted_at IS NULL',
        [$username]
    );
    
    if ($userData && password_verify($password, $userData['password_hash'])) {
        if (!$userData['active']) {
            $error = 'Account is disabled';
        } else {
            $_SESSION['user_id'] = $userData['id'];
            $_SESSION['username'] = $userData['username'];
            $_SESSION['role'] = $userData['role'];
            header('Location: index.php');
            exit;
        }
    } else {
        $error = 'Invalid credentials';
    }
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login - Retail POS</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #2980b9, #3498db); min-height: 100vh; display: flex; align-items: center; justify-content: center; }
        .login-container { background: white; padding: 40px; border-radius: 12px; box-shadow: 0 10px 40px rgba(0,0,0,0.2); width: 100%; max-width: 400px; }
        h1 { color: #2980b9; margin-bottom: 10px; text-align: center; }
        .subtitle { color: #666; text-align: center; margin-bottom: 30px; }
        .form-group { margin-bottom: 20px; }
        label { display: block; margin-bottom: 8px; font-weight: 600; color: #333; }
        input { width: 100%; padding: 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; }
        input:focus { outline: none; border-color: #2980b9; }
        button { width: 100%; background: #2980b9; color: white; padding: 12px; border: none; border-radius: 6px; cursor: pointer; font-size: 16px; font-weight: 600; }
        button:hover { background: #3498db; }
        .error { background: #fee; color: #c33; padding: 12px; border-radius: 6px; margin-bottom: 20px; text-align: center; }
    </style>
</head>
<body>
    <div class="login-container">
        <h1>Retail POS</h1>
        <p class="subtitle">Point of Sale System</p>
        
        <?php if ($error): ?>
            <div class="error"><?php echo htmlspecialchars($error); ?></div>
        <?php endif; ?>
        
        <form method="POST">
            <div class="form-group">
                <label>Username</label>
                <input type="text" name="username" required autofocus>
            </div>
            <div class="form-group">
                <label>Password</label>
                <input type="password" name="password" required>
            </div>
            <button type="submit">Login</button>
        </form>
    </div>
</body>
</html>
