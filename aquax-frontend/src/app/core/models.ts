export type RolUsuario = 'ADMINISTRADOR' | 'VENDEDOR';
export type EstadoEntrega = 'NUEVO' | 'EN_CAMINO' | 'ENTREGADO';
export type EstadoPago = 'PENDIENTE' | 'PAGADO';
export interface Usuario {
  id: number;
  nombreUsuario: string;
  nombre: string;
  apellido: string;
  correo: string;
  rol: RolUsuario;
  activo: boolean;
}
export interface Perfil extends Usuario {
  telefono: string | null;
}
export interface CrearUsuario {
  nombreUsuario: string;
  nombre: string;
  apellido: string;
  correo: string;
  contrasena: string;
  rol: RolUsuario;
}
export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  usuario: Usuario;
}
export interface Pagina<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}
export interface Producto {
  id: number;
  nombre: string;
  descripcion: string;
  precio: number;
}
export interface CrearPedido {
  nombreCliente: string;
  telefonoCliente: string;
  direccionEntrega: string;
  observaciones: string;
  detalles: { productoId: number; cantidad: number }[];
}
export interface Pedido {
  id: number;
  numeroPedido: string;
  nombreCliente: string;
  telefonoCliente: string;
  direccionEntrega: string;
  observaciones: string | null;
  fechaRegistro: string;
  fechaEntrega: string | null;
  origen: 'CLIENTE' | 'TRABAJADOR';
  estadoEntrega: EstadoEntrega;
  estadoPago: EstadoPago;
  cantidadProducto: number;
  total: number;
  detalles: {
    productoId: number;
    nombreProducto: string;
    cantidad: number;
    precioUnitario: number;
    subtotal: number;
  }[];
  usuarioRegistroId: number | null;
  usuarioRegistro: string | null;
}
export interface HistorialModificacion {
  id: number;
  pedidoId: number;
  usuarioId: number | null;
  usuario: string;
  campo: string;
  valorAnterior: string;
  valorNuevo: string;
  fecha: string;
}
export interface HistorialPagados {
  totalPagados: number;
  montoTotal: number;
  pedidos: Pedido[];
}
export interface ReporteCompletados {
  desde: string | null;
  hasta: string | null;
  totalPedidos: number;
  productos: { productoId: number; nombreProducto: string; cantidad: number }[];
  pedidos: Pedido[];
}
export interface DashboardResumen {
  totalPedidos: number;
  pendientes: number;
  pagados: number;
  deClientes: number;
}
export interface Empleado extends Perfil {
  dni: string;
  fechaIngreso: string;
  usuarioId: number;
}
export interface Empresa {
  id: number;
  razonSocial: string;
  ruc: string;
  direccion: string;
  telefono1: string | null;
  telefono2: string | null;
  correo: string | null;
  registroSanitario: string | null;
  slogan: string | null;
  ultimaActualizacion: string;
}
export interface Preferencias {
  usuarioId: number;
  idioma: string;
  nivelZoom: number;
  filtroColor: string;
  filtroColorActivo: boolean;
}
export interface Notificacion {
  id: number;
  pedidoId: number;
  numeroPedido: string;
  titulo: string;
  mensaje: string;
  tipo: string;
  leida: boolean;
  fechaCreacion: string;
}
export interface ProblemaApi {
  status: number;
  title: string;
  detail: string;
  instance: string;
  errores?: { campo: string; mensaje: string }[];
}
