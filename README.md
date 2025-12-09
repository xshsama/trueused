# TrueUsed Backend

## Alipay Sandbox Configuration

The project is configured to use Alipay Sandbox for payment testing.

### Configuration

Properties in `application.properties`:

- `alipay.app-id`: 9021000158632251
- `alipay.gateway-url`: https://openapi-sandbox.dl.alipaydev.com/gateway.do
- `alipay.notify-url`: http://localhost:8080/api/alipay/notify
- `alipay.return-url`: http://localhost:5173/payment/success

### Endpoints

- `POST /api/alipay/pay`: Create payment. Returns HTML form to auto-submit.
- `POST /api/alipay/notify`: Webhook for payment status updates.

### Testing

1. Login to Alipay Sandbox app with provided test account.
2. Create an order in TrueUsed.
3. Select Alipay on payment page.
4. Complete payment in the new window/tab.
5. Check console logs for "Order paid" message.
