<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/config.php';
require_once __DIR__ . '/../helpers/Response.php';
require_once __DIR__ . '/../middleware/AuthMiddleware.php';

class BackupController
{
    private const MAX_FILE_SIZE = 512 * 1024 * 1024; // 512 MB
    private const ALLOWED_EXTENSIONS = ['db', 'sqlite', 'zip', 'gz', 'sql', 'bak'];

    /**
     * POST /api/backup/upload
     * Multipart form-data, field: backup, optional field: terminal_id
     */
    public function upload(array $payload): never
    {
        AuthMiddleware::requireRole($payload, 'ADMIN');

        if (empty($_FILES['backup'])) {
            Response::error('No backup file uploaded. Use field name "backup".', 422);
        }

        $file    = $_FILES['backup'];
        $termId  = preg_replace('/[^a-zA-Z0-9_-]/', '_', $_POST['terminal_id'] ?? 'default');

        if ($file['error'] !== UPLOAD_ERR_OK) {
            $errorMsg = $this->uploadErrorMessage($file['error']);
            Response::error('Upload error: ' . $errorMsg, 400);
        }

        if ($file['size'] > self::MAX_FILE_SIZE) {
            Response::error('File too large. Maximum size is 512 MB.', 413);
        }

        $origName  = basename($file['name']);
        $ext       = strtolower(pathinfo($origName, PATHINFO_EXTENSION));

        if (!in_array($ext, self::ALLOWED_EXTENSIONS, true)) {
            Response::error('Invalid file type. Allowed: ' . implode(', ', self::ALLOWED_EXTENSIONS), 415);
        }

        $backupDir = BACKUP_DIR . DIRECTORY_SEPARATOR . $termId;
        if (!is_dir($backupDir)) {
            if (!mkdir($backupDir, 0750, true)) {
                Response::error('Failed to create backup directory', 500);
            }
        }

        // Safe filename: timestamp + sanitized original name
        $safeName  = date('Y-m-d_His') . '_' . preg_replace('/[^a-zA-Z0-9._-]/', '_', $origName);
        $destPath  = $backupDir . DIRECTORY_SEPARATOR . $safeName;

        if (!move_uploaded_file($file['tmp_name'], $destPath)) {
            Response::error('Failed to save uploaded file', 500);
        }

        // Build a relative URL
        $relativeUrl = '/backups/' . $termId . '/' . $safeName;

        Response::json([
            'url'         => $relativeUrl,
            'filename'    => $safeName,
            'size'        => $file['size'],
            'terminal_id' => $termId,
            'uploaded_at' => date('c'),
        ], 201);
    }

    /**
     * GET /api/backup/list
     * Lists backup files for a terminal.
     */
    public function listBackups(array $payload): never
    {
        AuthMiddleware::requireRole($payload, 'ADMIN');

        $termId    = preg_replace('/[^a-zA-Z0-9_-]/', '_', $_GET['terminal_id'] ?? 'default');
        $backupDir = BACKUP_DIR . DIRECTORY_SEPARATOR . $termId;

        if (!is_dir($backupDir)) {
            Response::json(['files' => []]);
        }

        $files = [];
        foreach (new DirectoryIterator($backupDir) as $fi) {
            if ($fi->isDot() || !$fi->isFile()) continue;
            $ext = strtolower($fi->getExtension());
            if (!in_array($ext, self::ALLOWED_EXTENSIONS, true)) continue;

            $files[] = [
                'filename'     => $fi->getFilename(),
                'size'         => $fi->getSize(),
                'url'          => '/backups/' . $termId . '/' . $fi->getFilename(),
                'modified_at'  => date('c', $fi->getMTime()),
            ];
        }

        usort($files, fn($a, $b) => strcmp($b['modified_at'], $a['modified_at']));

        Response::json(['files' => $files, 'terminal_id' => $termId]);
    }

    private function uploadErrorMessage(int $code): string
    {
        return match ($code) {
            UPLOAD_ERR_INI_SIZE   => 'File exceeds upload_max_filesize directive',
            UPLOAD_ERR_FORM_SIZE  => 'File exceeds MAX_FILE_SIZE directive',
            UPLOAD_ERR_PARTIAL    => 'File was only partially uploaded',
            UPLOAD_ERR_NO_FILE    => 'No file was uploaded',
            UPLOAD_ERR_NO_TMP_DIR => 'Missing temporary folder',
            UPLOAD_ERR_CANT_WRITE => 'Failed to write file to disk',
            UPLOAD_ERR_EXTENSION  => 'A PHP extension stopped the upload',
            default               => 'Unknown upload error',
        };
    }
}
