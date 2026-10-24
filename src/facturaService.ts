import {
  FacturaRequestDTO,
  FacturaReporteDTO,
  FacturaVentaDTO,
  ArticuloDTO,
  ClienteDTO,
  PaginaDTO,
  PuntoVentaDTO,
  TipoMonedaDTO,
} from "./types.js";

const API_URL = "http://localhost:8080/api/facturas";

export const facturaService = {
  crearFactura: async (
    facturaReq: FacturaRequestDTO,
  ): Promise<FacturaReporteDTO> => {
    const response = await fetch(API_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(facturaReq),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(
        errorData.message || `Error ${response.status}: ${response.statusText}`,
      );
    }
    return response.json();
  },

  obtenerFacturaPorId: async (id: number): Promise<FacturaVentaDTO> => {
    const response = await fetch(`${API_URL}/${id}`);
    if (!response.ok) {
      throw new Error(`No se pudo obtener la factura con ID ${id}`);
    }
    return response.json();
  },

  actualizarFactura: async (
    id: number,
    facturaReq: FacturaRequestDTO,
  ): Promise<FacturaReporteDTO> => {
    const response = await fetch(`${API_URL}/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(facturaReq),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(
        errorData.message || `Error al actualizar: ${response.statusText}`,
      );
    }
    return response.json();
  },

  anularFactura: async (id: number): Promise<FacturaReporteDTO> => {
    const response = await fetch(`${API_URL}/${id}/anular`, {
      method: "PUT",
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(
        errorData.message || `Error al anular: ${response.statusText}`,
      );
    }
    return response.json();
  },

  eliminarFactura: async (id: number): Promise<void> => {
    const response = await fetch(`${API_URL}/${id}`, {
      method: "DELETE",
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Error al eliminar factura.`);
    }
  },

  obtenerClientes: async (): Promise<ClienteDTO[]> => {
    const response = await fetch(`${API_URL}/clientes`);
    return response.ok ? response.json() : [];
  },

  obtenerArticulos: async (): Promise<ArticuloDTO[]> => {
    const response = await fetch(`${API_URL}/articulos`);
    return response.ok ? response.json() : [];
  },

  obtenerPuntosVenta: async (): Promise<PuntoVentaDTO[]> => {
    const response = await fetch(`${API_URL}/puntos-venta`);
    return response.ok ? response.json() : [];
  },

  obtenerTiposMoneda: async (): Promise<TipoMonedaDTO[]> => {
    const response = await fetch(`${API_URL}/tipos-moneda`);
    return response.ok ? response.json() : [];
  },

  descargarFacturaIndividualPdf: async (id: number): Promise<Blob> => {
    const response = await fetch(`${API_URL}/${id}/pdf`);
    if (!response.ok) {
      throw new Error(`Error al generar el PDF de la factura ID ${id}`);
    }
    return response.blob();
  },

  buscarFacturasPaginadas: async (filtros: {
    numeroFactura?: number;
    fechaDesde?: string;
    fechaHasta?: string;
    cliente?: string;
    estado?: string;
    informadaArca?: boolean;
    page?: number;
    size?: number;
  }): Promise<PaginaDTO<FacturaReporteDTO>> => {
    const params = new URLSearchParams();
    if (filtros.numeroFactura)
      params.append("numeroFactura", filtros.numeroFactura.toString());
    if (filtros.fechaDesde) params.append("fechaDesde", filtros.fechaDesde);
    if (filtros.fechaHasta) params.append("fechaHasta", filtros.fechaHasta);
    if (filtros.cliente) params.append("cliente", filtros.cliente);
    if (filtros.estado) params.append("estado", filtros.estado);
    if (filtros.informadaArca !== undefined)
      params.append("informadaArca", String(filtros.informadaArca));
    params.append("page", String(filtros.page || 0));
    params.append("size", String(filtros.size || 5));

    const response = await fetch(`${API_URL}?${params.toString()}`);
    if (!response.ok) throw new Error("Error al consultar las facturas.");
    return response.json();
  },
};
