// Ajusta el puerto y prefijo según la configuración de tu Spring Boot (application.properties)
const API_URL = 'http://localhost:8080/api/facturas';
export const facturaService = {
    // 1. Enviar la factura al backend
    crearFactura: async (facturaReq) => {
        const response = await fetch(`${API_URL}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(facturaReq),
        });
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(`Error al crear factura: ${errorData.message || response.statusText}`);
        }
        return response.json();
    },
    // 2. Obtener datos paramétricos para armar el formulario UI
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
    }
};
