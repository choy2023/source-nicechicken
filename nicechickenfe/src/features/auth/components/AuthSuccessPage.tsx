import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const AuthSuccessPage = () => {
  const navigate = useNavigate();

  useEffect(() => {
    navigate('/', { replace: true });
  }, [navigate]);

  return (
    <div className="flex h-screen items-center justify-center">
      <h2 className="text-xl font-bold text-gray-700">Processing login... Please wait.</h2>
    </div>
  );
};

export default AuthSuccessPage;