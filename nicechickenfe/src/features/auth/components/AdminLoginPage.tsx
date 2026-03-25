import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../../shared/api/axios';
import { AxiosError } from 'axios';
import styles from '../../admin/components/Admin.module.css';

const AdminLoginPage = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await api.post('/admin/login', { username, password });
      
      navigate('/admin');
    } catch (err: unknown) {
      const axiosError = err as AxiosError<{ error?: string }>;
      
      if (axiosError.response?.data?.error) {
        setError(axiosError.response.data.error);
      } else {
        setError('Login failed! Please check your username or password.');
      }
    }
  };

  return (
    <div className={styles.adminPage}>
      <div className={`${styles.adminCard} ${styles.loginCard}`}>
        <h2 className={styles.pageTitle}>🔐 Admin Login</h2>
        <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <input 
            type="text" 
            placeholder="Username (admin)"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className={styles.inputField}
          />
          <input 
            type="password" 
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={styles.inputField}
          />
          <button type="submit" className={styles.primaryBtn}>
            Login
          </button>
        </form>
        {error && <p className={styles.errorText}>{error}</p>}
      </div>
    </div>
  );
};

export default AdminLoginPage;