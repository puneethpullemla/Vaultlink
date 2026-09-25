import { useState } from "react";
import api from "../api";

export default function ShareModal({ file, onClose }) {
  const [expiryHours, setExpiryHours] = useState(24);
  const [link, setLink] = useState(null);
  const [error, setError] = useState("");
  const [copied, setCopied] = useState(false);

  const generateLink = async () => {
    setError("");
    try {
      const res = await api.post(`/files/${file.id}/share?expiryHours=${expiryHours}`);
      const url = `http://localhost:8080/api/share/${res.data.token}/download`;
      setLink({ url, expiresAt: res.data.expiresAt });
    } catch (err) {
      setError("Failed to generate link");
    }
  };

  const copyLink = () => {
    navigator.clipboard.writeText(link.url);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <h2>Share {file.originalName}</h2>

        {!link ? (
          <>
            <label>Expiration</label>
            <select value={expiryHours} onChange={(e) => setExpiryHours(Number(e.target.value))}>
              <option value={1}>1 hour</option>
              <option value={24}>1 day</option>
              <option value={168}>7 days</option>
            </select>
            {error && <p className="error">{error}</p>}
            <button onClick={generateLink}>Generate Link</button>
          </>
        ) : (
          <>
            <p className="link-success">Share link generated!</p>
            <div className="link-box">{link.url}</div>
            <p className="expiry-note">
              Expires: {new Date(link.expiresAt).toLocaleString()}
            </p>
            <button onClick={copyLink}>{copied ? "Copied!" : "Copy Link"}</button>
          </>
        )}

        <button className="close-btn" onClick={onClose}>Close</button>
      </div>
    </div>
  );
}
