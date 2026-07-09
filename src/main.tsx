import { createRoot } from "react-dom/client";
import App from "./App.tsx";
import { initAnalytics } from "./lib/analytics";
import "./index.css";

initAnalytics();
document.documentElement.classList.add("dark");
createRoot(document.getElementById("root")!).render(<App />);
