// API Helper
async function apiCall(endpoint, method = 'GET', body = null) {
    const headers = {
        'Content-Type': 'application/json',
    };
    
    if (TOKEN) {
        headers['Authorization'] = `Bearer ${TOKEN}`;
    }
    
    const options = {
        method,
        headers,
    };
    
    if (body) {
        options.body = JSON.stringify(body);
    }
    
    const response = await fetch(`${API_URL}${endpoint}`, options);
    const data = await response.json();
    
    if (!response.ok) {
        throw new Error(data.error || 'Request failed');
    }
    
    return data;
}

// Navigation - Wait for DOM to load
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.nav-links a').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const page = link.dataset.page;
            
            // Update active link
            document.querySelectorAll('.nav-links a').forEach(l => l.classList.remove('active'));
            link.classList.add('active');
            
            // Show page
            document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
            document.getElementById(`${page}-page`).classList.add('active');
            
            // Load page data
            loadPageData(page);
        });
    });
    
    // Initialize with sales page
    loadProducts();
});

function loadPageData(page) {
    switch(page) {
        case 'sales':
            loadProducts();
            break;
        case 'products':
            loadProductsTable();
            break;
        case 'customers':
            loadCustomersTable();
            break;
        case 'suppliers':
            loadSuppliersTable();
            break;
        case 'inventory':
            loadInventoryTable();
            break;
        case 'reports':
            loadReports();
            break;
    }
}

// Cart
let cart = [];

function addToCart(product) {
    const existingItem = cart.find(item => item.product_id === product.id);
    
    if (existingItem) {
        existingItem.quantity++;
    } else {
        cart.push({
            product_id: product.id,
            product_name: product.name,
            product_sku: product.sku,
            unit_price: product.selling_price,
            buying_price: product.buying_price,
            quantity: 1,
            discount: 0,
            tax_rate: product.tax_rate || 0,
        });
    }
    
    renderCart();
}

function removeFromCart(index) {
    cart.splice(index, 1);
    renderCart();
}

function updateCartQuantity(index, quantity) {
    if (quantity <= 0) {
        removeFromCart(index);
    } else {
        cart[index].quantity = quantity;
        renderCart();
    }
}

function renderCart() {
    const cartContainer = document.getElementById('cart-items');
    cartContainer.innerHTML = '';
    
    let subtotal = 0;
    let tax = 0;
    
    cart.forEach((item, index) => {
        const lineTotal = item.unit_price * item.quantity - item.discount;
        subtotal += lineTotal;
        tax += lineTotal * (item.tax_rate / 100);
        
        const cartItem = document.createElement('div');
        cartItem.className = 'cart-item';
        cartItem.innerHTML = `
            <div class="cart-item-info">
                <div class="cart-item-name">${item.product_name}</div>
                <div class="cart-item-qty">${item.quantity} × KES ${item.unit_price.toFixed(2)}</div>
            </div>
            <div class="cart-item-total">KES ${lineTotal.toFixed(2)}</div>
            <button class="btn btn-danger" onclick="removeFromCart(${index})">×</button>
        `;
        cartContainer.appendChild(cartItem);
    });
    
    const total = subtotal + tax;
    
    document.getElementById('subtotal').textContent = `KES ${subtotal.toFixed(2)}`;
    document.getElementById('tax').textContent = `KES ${tax.toFixed(2)}`;
    document.getElementById('total').textContent = `KES ${total.toFixed(2)}`;
}

// Products
async function loadProducts() {
    try {
        const response = await apiCall('/products');
        const products = response.data;
        
        const grid = document.getElementById('products-grid');
        grid.innerHTML = '';
        
        products.forEach(product => {
            const card = document.createElement('div');
            card.className = 'product-card';
            card.innerHTML = `
                <h3>${product.name}</h3>
                <div class="price">KES ${product.selling_price.toFixed(2)}</div>
                <div class="stock ${product.current_stock <= product.minimum_stock ? 'low-stock' : ''}">
                    Stock: ${product.current_stock}
                </div>
            `;
            card.addEventListener('click', () => addToCart(product));
            grid.appendChild(card);
        });
    } catch (error) {
        console.error('Failed to load products:', error);
    }
}

async function loadProductsTable() {
    try {
        const response = await apiCall('/products');
        const products = response.data;
        
        const tbody = document.getElementById('products-table-body');
        tbody.innerHTML = '';
        
        products.forEach(product => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${product.name}</td>
                <td>${product.sku}</td>
                <td>KES ${product.selling_price.toFixed(2)}</td>
                <td class="${product.current_stock <= product.minimum_stock ? 'low-stock' : ''}">${product.current_stock}</td>
                <td>${product.status}</td>
                <td>
                    <button class="btn btn-secondary" onclick="editProduct('${product.id}')">Edit</button>
                    <button class="btn btn-danger" onclick="deleteProduct('${product.id}')">Delete</button>
                </td>
            `;
            tbody.appendChild(row);
        });
    } catch (error) {
        console.error('Failed to load products:', error);
    }
}

// Customers
async function loadCustomersTable() {
    try {
        const response = await apiCall('/customers');
        const customers = response.data;
        
        const tbody = document.getElementById('customers-table-body');
        tbody.innerHTML = '';
        
        customers.forEach(customer => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${customer.name}</td>
                <td>${customer.phone || '-'}</td>
                <td>${customer.email || '-'}</td>
                <td>${customer.loyalty_points}</td>
                <td>
                    <button class="btn btn-secondary" onclick="editCustomer('${customer.id}')">Edit</button>
                    <button class="btn btn-danger" onclick="deleteCustomer('${customer.id}')">Delete</button>
                </td>
            `;
            tbody.appendChild(row);
        });
    } catch (error) {
        console.error('Failed to load customers:', error);
    }
}

// Suppliers
async function loadSuppliersTable() {
    try {
        const response = await apiCall('/suppliers');
        const suppliers = response.data;
        
        const tbody = document.getElementById('suppliers-table-body');
        tbody.innerHTML = '';
        
        suppliers.forEach(supplier => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${supplier.name}</td>
                <td>${supplier.phone || '-'}</td>
                <td>${supplier.email || '-'}</td>
                <td>KES ${supplier.balance.toFixed(2)}</td>
                <td>
                    <button class="btn btn-secondary" onclick="editSupplier('${supplier.id}')">Edit</button>
                    <button class="btn btn-danger" onclick="deleteSupplier('${supplier.id}')">Delete</button>
                </td>
            `;
            tbody.appendChild(row);
        });
    } catch (error) {
        console.error('Failed to load suppliers:', error);
    }
}

// Inventory
async function loadInventoryTable() {
    try {
        const response = await apiCall('/products');
        const products = response.data;
        
        const tbody = document.getElementById('inventory-table-body');
        tbody.innerHTML = '';
        
        products.forEach(product => {
            const row = document.createElement('tr');
            const status = product.current_stock <= product.minimum_stock ? 'Low Stock' : 'OK';
            row.innerHTML = `
                <td>${product.name}</td>
                <td class="${product.current_stock <= product.minimum_stock ? 'low-stock' : ''}">${product.current_stock}</td>
                <td>${product.minimum_stock}</td>
                <td class="${product.current_stock <= product.minimum_stock ? 'low-stock' : ''}">${status}</td>
            `;
            tbody.appendChild(row);
        });
    } catch (error) {
        console.error('Failed to load inventory:', error);
    }
}

// Reports
async function loadReports() {
    try {
        const today = new Date().toISOString().split('T')[0];
        const response = await apiCall(`/sales?start_date=${today}&end_date=${today}`);
        const sales = response.data;
        
        const todayTotal = sales.reduce((sum, sale) => sum + sale.grand_total, 0);
        document.getElementById('today-sales').textContent = `KES ${todayTotal.toFixed(2)}`;
        
        // TODO: Load week and month reports
        document.getElementById('week-sales').textContent = 'KES 0.00';
        document.getElementById('month-sales').textContent = 'KES 0.00';
    } catch (error) {
        console.error('Failed to load reports:', error);
    }
}

// Settings, Cart actions - Wait for DOM
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('save-settings-btn')?.addEventListener('click', async () => {
        try {
            await apiCall('/settings', 'PUT', {
                store_name: document.getElementById('setting-store-name').value,
                store_address: document.getElementById('setting-store-address').value,
                store_phone: document.getElementById('setting-store-phone').value,
                tax_rate: document.getElementById('setting-tax-rate').value,
            });
            alert('Settings saved successfully');
        } catch (error) {
            alert('Failed to save settings: ' + error.message);
        }
    });

    document.getElementById('clear-cart-btn')?.addEventListener('click', () => {
        cart = [];
        renderCart();
    });

    document.getElementById('pay-btn')?.addEventListener('click', async () => {
        if (cart.length === 0) {
            alert('Cart is empty');
            return;
        }
        
        try {
            const response = await apiCall('/sales', 'POST', {
                items: cart,
                payment_method: 'CASH',
                cash_tendered: parseFloat(document.getElementById('total').textContent.replace('KES ', '')),
            });
            
            alert(`Sale completed! Receipt: ${response.receipt_number}`);
            cart = [];
            renderCart();
            loadProducts();
        } catch (error) {
            alert('Failed to complete sale: ' + error.message);
        }
    });
});

// Product search
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('product-search')?.addEventListener('input', async (e) => {
        const search = e.target.value;
        try {
            const response = await apiCall(`/products?search=${search}`);
            const products = response.data;
            
            const grid = document.getElementById('products-grid');
            grid.innerHTML = '';
            
            products.forEach(product => {
                const card = document.createElement('div');
                card.className = 'product-card';
                card.innerHTML = `
                    <h3>${product.name}</h3>
                    <div class="price">KES ${product.selling_price.toFixed(2)}</div>
                    <div class="stock ${product.current_stock <= product.minimum_stock ? 'low-stock' : ''}">
                        Stock: ${product.current_stock}
                    </div>
                `;
                card.addEventListener('click', () => addToCart(product));
                grid.appendChild(card);
            });
        } catch (error) {
            console.error('Search failed:', error);
        }
    });
});
