import { Routes } from '@angular/router';
import { authGuard, adminGuard } from './core/auth/auth.guards';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/catalogo/catalogo.page').then((m) => m.CatalogoPage),
    title: 'AquaX · Agua que cuida',
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.page').then((m) => m.LoginPage),
    title: 'Iniciar sesión · AquaX',
  },
  {
    path: 'panel',
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    loadComponent: () => import('./layout/panel.layout').then((m) => m.PanelLayout),
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () =>
          import('./features/dashboard/dashboard.page').then((m) => m.DashboardPage),
        title: 'Resumen · AquaX',
      },
      {
        path: 'pedidos/nuevo',
        loadComponent: () =>
          import('./features/catalogo/nuevo-pedido.page').then((m) => m.NuevoPedidoPage),
        title: 'Nuevo pedido · AquaX',
      },
      {
        path: 'pedidos',
        loadComponent: () => import('./features/pedidos/pedidos.page').then((m) => m.PedidosPage),
        title: 'Pedidos · AquaX',
      },
      {
        path: 'reportes',
        loadComponent: () =>
          import('./features/reportes/reportes.page').then((m) => m.ReportesPage),
        title: 'Reportes · AquaX',
      },
      {
        path: 'notificaciones',
        loadComponent: () =>
          import('./features/notificaciones/notificaciones.page').then((m) => m.NotificacionesPage),
        title: 'Notificaciones · AquaX',
      },
      {
        path: 'perfil',
        loadComponent: () => import('./features/cuenta/perfil.page').then((m) => m.PerfilPage),
        title: 'Mi perfil · AquaX',
      },
      {
        path: 'preferencias',
        loadComponent: () =>
          import('./features/cuenta/preferencias.page').then((m) => m.PreferenciasPage),
        title: 'Preferencias · AquaX',
      },
      {
        path: 'usuarios',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/admin/usuarios.page').then((m) => m.UsuariosPage),
        title: 'Usuarios · AquaX',
      },
      {
        path: 'empleados',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/admin/empleados.page').then((m) => m.EmpleadosPage),
        title: 'Empleados · AquaX',
      },
      {
        path: 'empresa',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/admin/empresa.page').then((m) => m.EmpresaPage),
        title: 'Empresa · AquaX',
      },
    ],
  },
  {
    path: '**',
    loadComponent: () => import('./shared/not-found.page').then((m) => m.NotFoundPage),
    title: 'Página no encontrada · AquaX',
  },
];
