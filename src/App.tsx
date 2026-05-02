import { lazy, Suspense } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import { AuthProvider } from "@/hooks/use-auth";
import { RouteSeo } from "@/components/layout/RouteSeo";

const Home = lazy(() => import("./pages/Home"));
const Entrar = lazy(() => import("./pages/Entrar"));
const Calendario = lazy(() => import("./pages/Calendario"));
const Explorar = lazy(() => import("./pages/Explorar"));
const Perfil = lazy(() => import("./pages/Perfil"));
const Anime = lazy(() => import("./pages/Anime"));
const Watch = lazy(() => import("./pages/Watch"));
const Notificacoes = lazy(() => import("./pages/Notificacoes"));
const NotFound = lazy(() => import("./pages/NotFound"));
const TermosDeUso = lazy(() => import("./pages/TermosDeUso"));
const PoliticaPrivacidade = lazy(() => import("./pages/PoliticaPrivacidade"));

const AdminLayout = lazy(() => import("./pages/admin/AdminLayout"));
const AdminDashboard = lazy(() => import("./pages/admin/AdminIndex"));
const AdminAnimes = lazy(() => import("./pages/admin/AdminAnimes"));
const AdminEpisodes = lazy(() => import("./pages/admin/AdminEpisodios"));
const AdminComments = lazy(() => import("./pages/admin/AdminComentarios"));
const AdminSuggestions = lazy(() => import("./pages/admin/AdminSugestoes"));
const AdminHome = lazy(() => import("./pages/admin/AdminHome"));
const AdminCalendario = lazy(() => import("./pages/admin/AdminCalendario"));

const queryClient = new QueryClient();

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <BrowserRouter>
        <AuthProvider>
          <RouteSeo />
          <Suspense fallback={<div className="min-h-screen bg-background" />}>
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/entrar" element={<Entrar />} />
              <Route path="/calendario" element={<Calendario />} />
              <Route path="/explorar" element={<Explorar />} />
              <Route path="/perfil" element={<Perfil />} />
              <Route path="/anime/:slug" element={<Anime />} />
              <Route path="/watch/:slug/:episodeNumber" element={<Watch />} />
              <Route path="/notificacoes" element={<Notificacoes />} />
              <Route path="/termos-de-uso" element={<TermosDeUso />} />
              <Route path="/politica-de-privacidade" element={<PoliticaPrivacidade />} />

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
          </Suspense>
        </AuthProvider>
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
