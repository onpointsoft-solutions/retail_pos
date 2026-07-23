<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo isset($pageTitle) ? htmlspecialchars($pageTitle) : 'Victorious General Shop'; ?></title>
    
    <!-- Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;500;600;700;800&family=Poppins:wght@400;500;600;700&display=swap" rel="stylesheet">
    
    <!-- Tailwind CSS CDN -->
    <script src="https://cdn.tailwindcss.com"></script>
    
    <!-- Tailwind Config -->
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        'primary-gold': '#F59E0B',
                        'gold-light': '#FCD34D',
                        'accent-violet': '#7C3AED',
                        'surface-gray': '#F9FAFB',
                        'charcoal': '#1F2937',
                        'neutral-white': '#FFFFFF'
                    },
                    fontFamily: {
                        heading: ['Montserrat', 'sans-serif'],
                        body: ['Poppins', 'sans-serif']
                    }
                }
            }
        }
    </script>
    
    <!-- Alpine.js -->
    <script src="https://unpkg.com/alpinejs@3.x.x/dist/cdn.min.js" defer></script>
    
    <!-- Custom CSS -->
    <link rel="stylesheet" href="../assets/css/custom.css">
    
    <style>
        body {
            font-family: 'Poppins', sans-serif;
        }
        h1, h2, h3, h4, h5, h6 {
            font-family: 'Montserrat', sans-serif;
        }
    </style>
</head>
<body class="bg-surface-gray text-charcoal" x-data="{ mobileMenuOpen: false, searchOpen: false }">
    
    <!-- Sticky Navigation Bar -->
    <nav class="sticky top-0 z-50 bg-neutral-white shadow-sm">
        <div class="container mx-auto px-4 py-4 flex items-center justify-between" style="height: 72px;">
            
            <!-- Logo and Brand Name -->
            <div class="flex items-center space-x-3">
                <svg class="w-8 h-8 text-primary-gold" fill="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path d="M3 3h18v2H3V3zm0 4h18v2H3V7zm0 4h18v2H3v-2zm0 4h18v2H3v-2zm0 4h18v2H3v-2z"/>
                    <path d="M20 8l-1.5 12h-13L4 8h16zm-2 2H6l1.2 9.6h9.6L18 10z"/>
                </svg>
                <a href="index.php" class="font-heading font-bold text-charcoal text-lg">Victorious General Shop</a>
            </div>
            
            <!-- Desktop Navigation Links (Center) -->
            <div class="hidden md:flex items-center space-x-8">
                <a href="index.php" class="font-body font-medium text-charcoal hover:text-primary-gold transition-colors duration-200 relative group">
                    Home
                    <span class="absolute bottom-0 left-0 w-0 h-0.5 bg-primary-gold transition-all duration-300 group-hover:w-full"></span>
                </a>
                <a href="shop.php" class="font-body font-medium text-charcoal hover:text-primary-gold transition-colors duration-200 relative group">
                    Shop
                    <span class="absolute bottom-0 left-0 w-0 h-0.5 bg-primary-gold transition-all duration-300 group-hover:w-full"></span>
                </a>
                <a href="about.php" class="font-body font-medium text-charcoal hover:text-primary-gold transition-colors duration-200 relative group">
                    About
                    <span class="absolute bottom-0 left-0 w-0 h-0.5 bg-primary-gold transition-all duration-300 group-hover:w-full"></span>
                </a>
                <a href="contact.php" class="font-body font-medium text-charcoal hover:text-primary-gold transition-colors duration-200 relative group">
                    Contact
                    <span class="absolute bottom-0 left-0 w-0 h-0.5 bg-primary-gold transition-all duration-300 group-hover:w-full"></span>
                </a>
            </div>
            
            <!-- Right Side Icons -->
            <div class="flex items-center space-x-4">
                <!-- Search Icon -->
                <button @click="searchOpen = !searchOpen" class="text-charcoal hover:text-primary-gold transition-colors">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                    </svg>
                </button>
                
                <!-- Cart Icon with Badge -->
                <a href="cart.php" class="relative text-charcoal hover:text-primary-gold transition-colors">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"></path>
                    </svg>
                    <span id="cart-badge" class="absolute -top-2 -right-2 bg-accent-violet text-neutral-white text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center">0</span>
                </a>
                
                <!-- Mobile Menu Toggle -->
                <button @click="mobileMenuOpen = !mobileMenuOpen" class="md:hidden text-charcoal">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
                    </svg>
                </button>
            </div>
        </div>
        
        <!-- Search Bar (Toggleable) -->
        <div x-show="searchOpen" x-transition class="border-t border-gray-200 bg-neutral-white">
            <div class="container mx-auto px-4 py-3">
                <input type="text" placeholder="Search products..." class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-gold">
            </div>
        </div>
        
        <!-- Mobile Menu -->
        <div x-show="mobileMenuOpen" x-transition class="md:hidden border-t border-gray-200 bg-neutral-white">
            <div class="container mx-auto px-4 py-4 flex flex-col space-y-4">
                <a href="index.php" class="font-body font-medium text-charcoal hover:text-primary-gold py-2" style="min-height: 44px;">Home</a>
                <a href="shop.php" class="font-body font-medium text-charcoal hover:text-primary-gold py-2" style="min-height: 44px;">Shop</a>
                <a href="about.php" class="font-body font-medium text-charcoal hover:text-primary-gold py-2" style="min-height: 44px;">About</a>
                <a href="contact.php" class="font-body font-medium text-charcoal hover:text-primary-gold py-2" style="min-height: 44px;">Contact</a>
            </div>
        </div>
    </nav>
    
    <!-- Floating WhatsApp Button -->
    <a href="https://wa.me/254742071810?text=Hi%20Victorious%20General%20Shop%2C%20I%27d%20like%20to%20ask%20about%20a%20product" 
       target="_blank" 
       class="fixed bottom-6 right-6 z-50 w-14 h-14 bg-[#25D366] rounded-full flex items-center justify-center shadow-lg hover:scale-110 transition-transform duration-200">
        <svg class="w-8 h-8 text-neutral-white" fill="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413Z"/>
        </svg>
    </a>
    
    <!-- Main Content Starts Here -->
    
    <!-- Cart JS -->
    <script src="../assets/js/cart.js"></script>
