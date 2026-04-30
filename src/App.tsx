import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import { AuthProvider } from "@/hooks/use-auth";

import Home from "./pages/Home";
import Entrar from "./pages/Entrar";
import Calendario from "./pages/Calendario";
import Perfil from "./pages/Perfil";
import Anime from "./pages/Anime";
import Watch from "./pages/Watch";
import NotFound from "./pages/NotFound";

import AdminLayout from "./pages/admin/AdminLayout";
import AdminDashboard from "./pages/admin/AdminIndex";
import AdminAnimes from "./pages/admin/AdminAnimes";
import AdminEpisodes from "./pages/admin/AdminEpisodios";
import AdminComments from "./pages/admin/AdminComentarios";
import AdminSuggestions from "./pages/admin/AdminSugestoes";
import AdminHome from "./pages/admin/AdminHome";
import AdminCalendario from "./pages/admin/AdminCalendario";

const queryClient = new QueryClient();

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/entrar" element={<Entrar />} />
            <Route path="/calendario" element={<Calendario />} />
            <Route path="/perfil" element={<Perfil />} />
            <Route path="/anime/:slug" element={<Anime />} />
            <Route path="/watch/:slug/:episodeNumber" element={<Watch />} />

            <Route path="/admin" element={<AdminLayout />}>
              <Route index element={<AdminDashboard />} />
              <Route path="home" element={<AdminHome />} />
              <Route path="calendario" element={<AdminCalendario />} />
              <Route path="animes" element={<AdminAnimes />} />
              <Route path="episodios" element={<AdminEpisodes />} />
              <Route path="comentarios" element={<AdminComments />} />
              <Route path="sugestoes" element={<AdminSuggestions />} />
            </Route>

            <Route path="*" element={<NotFound />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
