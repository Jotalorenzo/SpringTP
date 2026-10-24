import {
  FacturaRequestDTO,
  FacturaReporteDTO,
  ArticuloDTO,
  ClienteDTO,
  PuntoVentaDTO,
  TipoMonedaDTO,
} from "./types.js";

// URL base limpia (sin duplicar /facturas)
const API_URL = "http://localhost:8080/api/facturas";

export const facturaService = {
  // 1. Enviar la factura al backend (POST http://localhost:8080/api/facturas)
  crearFactura: async (
    facturaReq: FacturaRequestDTO,
  ): Promise<FacturaReporteDTO> => {
    const response = await fetch(API_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
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

  // 2. Obtener datos paramétricos
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
};
