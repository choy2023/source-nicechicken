import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import OrderPage from './features/orders/components/OrderPage';
import AdminLoginPage from './features/auth/components/AdminLoginPage';
import AdminDashboard from './features/admin/components/AdminDashboard';
import AuthSuccessPage from './features/auth/components/AuthSuccessPage';
import PaymentSuccessPage from './features/payments/components/PaymentSuccessPage';
import PaymentCancelPage from './features/payments/components/PaymentCancelPage';

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<OrderPage />} />
          <Route path="/admin/login" element={<AdminLoginPage />} />
          <Route path="/auth/success" element={<AuthSuccessPage />} />
          <Route path="/payment/success" element={<PaymentSuccessPage />} />
          <Route path="/payment/cancel" element={<PaymentCancelPage />} />
          <Route path="/admin" element={<AdminDashboard />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;