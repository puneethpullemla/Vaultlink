import { createContext, useContext, useState } from "react";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [userId, setUserId] = useState(localStorage.getItem("userId"));
  const [email, setEmail] = useState(localStorage.getItem("email"));

  const login = (token, id, userEmail) => {
    localStorage.setItem("token", token);
    localStorage.setItem("userId", id);
    localStorage.setItem("email", userEmail);
    setUserId(id);
    setEmail(userEmail);
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("userId");
    localStorage.removeItem("email");
    setUserId(null);
    setEmail(null);
  };

  const isLoggedIn = !!localStorage.getItem("token");

  return (
    <AuthContext.Provider value={{ userId, email, isLoggedIn, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
