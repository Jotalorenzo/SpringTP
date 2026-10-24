const API_URL = "http://localhost:8080/api/facturas";
export const facturaService = {
    crearFactura: async (facturaReq) => {
        const response = await fetch(API_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(facturaReq),
        });
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `Error ${response.status}: ${response.statusText}`);
        }
        return response.json();
    },
    obtenerFacturaPorId: async (id) => {
        const response = await fetch(`${API_URL}/${id}`);
        if (!response.ok) {
            throw new Error(`No se pudo obtener la factura con ID ${id}`);
        }
        return response.json();
    },
    actualizarFactura: async (id, facturaReq) => {
        const response = await fetch(`${API_URL}/${id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(facturaReq),
        });
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `Error al actualizar: ${response.statusText}`);
        }
        return response.json();
    },
    anularFactura: async (id) => {
        const response = await fetch(`${API_URL}/${id}/anular`, {
            method: "PUT",
        });
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `Error al anular: ${response.statusText}`);
        }
        return response.json();
    },
    eliminarFactura: async (id) => {
        const response = await fetch(`${API_URL}/${id}`, {
            method: "DELETE",
        });
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `Error al eliminar factura.`);
        }
    },
    obtenerClientes: async () => {
        const response = await fetch(`${API_URL}/clientes`);
        return response.ok ? response.json() : [];
    },
    obtenerArticulos: async () => {
        const response = await fetch(`${API_URL}/articulos`);
        return response.ok ? response.json() : [];
    },
    obtenerPuntosVenta: async () => {
        const response = await fetch(`${API_URL}/puntos-venta`);
        return response.ok ? response.json() : [];
    },
    obtenerTiposMoneda: async () => {
        const response = await fetch(`${API_URL}/tipos-moneda`);
        return response.ok ? response.json() : [];
    },
};
