<?php
/**
 * BizFlow POS – reusable logo partial.
 *
 * Usage:
 *   <?php require __DIR__ . '/logo.php'; ?>
 *   <?= bizflowLogo() ?>                    // default size
 *   <?= bizflowLogo(height: 48) ?>          // custom height
 *   <?= bizflowLogo(linkTo: '/')  ?>        // wrapped in <a>
 *
 * The file also exposes the raw <img> tag via bizflowLogoImg() for cases
 * where the surrounding markup handles the link itself.
 */
declare(strict_types=1);

if (!function_exists('bizflowLogoImg')) {
    function bizflowLogoImg(int $height = 44, string $extraClass = ''): string
    {
        $src = htmlspecialchars(
            rtrim(getenv('LICENSE_SITE_URL') ?: '', '/') . '/logo.png'
            ?: 'logo.png'
        );
        // Fall back to relative path when no env URL is configured
        $src = 'logo.png';
        $cls = trim('h-auto object-contain ' . $extraClass);
        return sprintf(
            '<img src="%s" alt="BizFlow POS" height="%d" class="%s" style="height:%dpx;width:auto">',
            $src,
            $height,
            htmlspecialchars($cls),
            $height
        );
    }
}

if (!function_exists('bizflowLogo')) {
    /**
     * Returns the logo – optionally wrapped in an <a> tag.
     *
     * @param int         $height   Logo render height in pixels (default 44)
     * @param string|null $linkTo   If set, wraps the image in <a href="$linkTo">
     * @param string      $extraClass Additional CSS classes on the <img>
     */
    function bizflowLogo(int $height = 44, ?string $linkTo = null, string $extraClass = ''): string
    {
        $img = bizflowLogoImg($height, $extraClass);
        if ($linkTo !== null) {
            $href = htmlspecialchars($linkTo);
            return "<a href=\"{$href}\" class=\"inline-flex items-center shrink-0\">{$img}</a>";
        }
        return $img;
    }
}
