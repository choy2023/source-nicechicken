import React from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './Payment.module.css';

const PaymentSuccessPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className={styles.paymentPage}>
      <div className={styles.paymentCard}>
        <div className={styles.successIcon}>✅</div>
        <h1 className={styles.title}>Payment Successful!</h1>
        <p className={styles.message}>Your order has been successfully placed and sent to the kitchen.</p>
        <button 
          onClick={() => navigate('/')}
          className={styles.homeBtn}
        >
          Back to Home
        </button>
      </div>
    </div>
  );
};

export default PaymentSuccessPage;