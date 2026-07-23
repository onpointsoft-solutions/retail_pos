# Desktop POS production integration

Use JWT authentication, not browser cookies. Set the desktop base URL to
`https://your-domain/api/`. Obtain tokens with `POST /api/auth/token/` using
`{"username":"…","password":"…"}` and send `Authorization: Bearer <access>`.
Refresh tokens at `/api/auth/token/refresh/`; never store tokens in plain text.

| Method | Endpoint | Access | Purpose |
| --- | --- | --- | --- |
| GET | `/api/pos/bootstrap/` | POS staff / owner | Restaurant, shift, menu, orders |
| POST | `/api/pos/orders/` | Waiter | Create an order from server-priced items |
| POST | `/api/pos/orders/{id}/invoice/` | Waiter | Send an order to the cashier |
| POST | `/api/pos/orders/{id}/pay/` | Cashier | Complete cash, M-Pesa, or card payment |
| POST | `/api/pos/sessions/close/` | Owner | Reconcile and close a shift |

The Django API calculates totals, uses transactions for payment, and locks the
restaurant during receipt-number allocation. Before deployment, use HTTPS,
`DEBUG=False`, environment-managed secrets, restrictive allowed hosts/CORS,
database backups, and authentication/payment monitoring.
