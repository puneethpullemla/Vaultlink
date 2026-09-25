import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api";
import { useAuth } from "../AuthContext";
import ShareModal from "../components/ShareModal";

function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default function Dashboard() {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");
  const [shareTarget, setShareTarget] = useState(null);
  const fileInputRef = useRef(null);
  const { email, logout } = useAuth();
  const navigate = useNavigate();

  const loadFiles = async () => {
    try {
      const res = await api.get("/files");
      setFiles(res.data);
    } catch (err) {
      setError("Could not load files");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadFiles();
  }, []);

  const handleUploadClick = () => fileInputRef.current.click();

  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setUploading(true);
    setError("");
    const formData = new FormData();
    formData.append("file", file);

    try {
      await api.post("/files/upload", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      await loadFiles();
    } catch (err) {
      setError(err.response?.data?.message || "Upload failed");
    } finally {
      setUploading(false);
      e.target.value = "";
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Delete this file?")) return;
    try {
      await api.delete(`/files/${id}`);
      setFiles(files.filter((f) => f.id !== id));
    } catch (err) {
      setError("Delete failed");
    }
  };

  const handleView = async (file) => {
    try {
      const res = await api.get(`/files/${file.id}/view`, { responseType: "blob" });
      const blobUrl = URL.createObjectURL(res.data);
      window.open(blobUrl, "_blank");
    } catch (err) {
      setError("Could not open file");
    }
  };

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div className="dashboard">
      <header className="topbar">
        <h1>VaultLink</h1>
        <div className="user-menu">
          <span>{email}</span>
          <button onClick={handleLogout}>Log out</button>
        </div>
      </header>

      <div className="dashboard-body">
        <div className="dashboard-header">
          <h2>My Files</h2>
          <button onClick={handleUploadClick} disabled={uploading}>
            {uploading ? "Uploading..." : "Upload File"}
          </button>
          <input
            type="file"
            ref={fileInputRef}
            style={{ display: "none" }}
            onChange={handleFileChange}
          />
        </div>

        {error && <p className="error">{error}</p>}

        {loading ? (
          <p>Loading...</p>
        ) : files.length === 0 ? (
          <p className="empty-state">No files yet. Upload your first file to get started.</p>
        ) : (
          <div className="file-list">
            {files.map((file) => (
              <div className="file-card" key={file.id}>
                <div className="file-info">
                  <span className="file-name">{file.originalName}</span>
                  <span className="file-meta">{formatBytes(file.fileSize)}</span>
                </div>
                <div className="file-actions">
                  <button onClick={() => setShareTarget(file)}>Share</button>
                  <button onClick={() => handleView(file)}>View</button>
                  <button className="danger" onClick={() => handleDelete(file.id)}>
                    Delete
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {shareTarget && (
        <ShareModal file={shareTarget} onClose={() => setShareTarget(null)} />
      )}
    </div>
  );
}