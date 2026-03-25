import React from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './Payment.module.css';

const PaymentCancelPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className={styles.paymentPage}>
      <div className={styles.paymentCard}>
        <div className={styles.errorIcon}>❌</div>
        <h1 className={styles.title}>Payment Cancelled!</h1>
        <p className={styles.message}>Your order has been cancelled. Please try again.</p>
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

export default PaymentCancelPage;