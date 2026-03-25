import api from "../../../shared/api/axios";

export const cancelOrder = async (orderId: string) => {
  const response = await api.post(`/orders/${orderId}/cancel`);
  return response.data;
};

export const hideOrder = async (orderId: string) => {
  const response = await api.patch(`/orders/${orderId}/hide`);
  return response.data;
};
