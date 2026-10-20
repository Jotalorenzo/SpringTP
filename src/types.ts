// ==========================================
// DTOs de Dominio / Entidades Auxiliares
// ==========================================

export interface ClienteDTO {
    id: number;
    razonSocial?: string;
    nombre?: string;
    cuit?: string;
}

export interface ArticuloDTO {
    id: number;
    codigo?: string;
    denominacion?: string;
    precioVenta?: number;
}

export interface PuntoVentaDTO {
    id: number;
    numero: number;
    denominacion?: string;
}

export interface TipoMonedaDTO {
    id: number;
    nombre: string;
    simbolo?: string;
}

export interface ListaPrecioDTO {
    id: number;
    nombre: string;
}

// ==========================================
// DTOs de Solicitud (Request Payload)
// ==========================================

export interface FacturaDetalleRequestDTO {
    articuloId: number;
    cantidad: number;
    precioUnitario?: number;
}

export interface FacturaRequestDTO {
    clienteId: number;
    puntoVentaId: number;
    tipoMonedaId: number;
    listaPrecioId?: number;
    detalles: FacturaDetalleRequestDTO[];
}

// ==========================================
// DTOs de Respuesta (Response Payload)
// ==========================================

export interface FacturaVentaDetalleDTO {
    id?: number;
    articulo: ArticuloDTO;
    cantidad: number;
    precioUnitario: number;
    subtotal: number;
}

export interface FacturaVentaDTO {
    id: number;
    numeroFactura?: string;
    fecha?: string;
    cliente: ClienteDTO;
    puntoVenta: PuntoVentaDTO;
    total: number;
    detalles: FacturaVentaDetalleDTO[];
}