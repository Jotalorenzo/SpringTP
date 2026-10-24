export interface ClienteDTO {
  id: number;
  cuitCuil?: string;
  denominacion?: string;
}

export interface ArticuloDTO {
  id: number;
  codigo?: string;
  denominacion?: string;
}

export interface PuntoVentaDTO {
  id: number;
  numero: number;
  descripcion?: string;
}

export interface TipoMonedaDTO {
  id: number;
  codigoAfip?: string;
  denominacion: string;
  simbolo?: string;
}

export interface FacturaDetalleRequestDTO {
  articuloId: number;
  cantidad: number;
  descripcion?: string;
  porcentajeBonificacion?: number;
}

export interface FacturaRequestDTO {
  numero: number;
  fechaEmision: string; // formato YYYY-MM-DD
  puntoVentaId: number;
  condicionIvaId: number;
  tipoMonedaId: number;
  clienteId?: number | null;
  estado?: string;
  observaciones?: string;
  detalles: FacturaDetalleRequestDTO[];
}

export interface FacturaReporteDTO {
  numeroFactura: number;
  fechaEmision: string;
  clienteDenominacion: string;
  condicionIva: string;
  puntoVentaDescripcion: string;
  importeTotal: number;
  cantidadItems: number;
}
