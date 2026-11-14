const API_URL = "http://localhost:8080/api/facturas";
export const facturaService = {
    crearFactura: async (req) => {
        const res = await fetch(API_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(req),
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.message || err.error || `Error ${res.status}`);
        }
        return res.json();
    },
    actualizarFactura: async (id, req) => {
        const res = await fetch(`${API_URL}/${id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(req),
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.message || err.error || `Error ${res.status}`);
        }
        return res.json();
    },
    obtenerFacturaPorId: async (id) => {
        const res = await fetch(`${API_URL}/${id}`);
        if (!res.ok)
            throw new Error(`No se pudo obtener la factura ${id}`);
        return res.json();
    },
    anularFactura: async (id) => {
        const res = await fetch(`${API_URL}/${id}/anular`, { method: "PUT" });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.error || err.message || "Error al anular");
        }
        return res.json();
    },
    eliminarFactura: async (id) => {
        const res = await fetch(`${API_URL}/${id}`, { method: "DELETE" });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.error || err.message || "Error al eliminar");
        }
    },
    informarArca: async (id) => {
        const res = await fetch(`${API_URL}/${id}/informar-arca`, {
            method: "PUT",
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.error || err.message || "Error al informar a ARCA");
        }
        return res.json();
    },
    descargarFacturaIndividualPdf: async (id) => {
        const res = await fetch(`${API_URL}/${id}/pdf`);
        if (!res.ok)
            throw new Error("Error al descargar PDF");
        return res.blob();
    },
    buscarFacturasPaginadas: async (filtros) => {
        const params = new URLSearchParams();
        if (filtros.numeroFactura)
            params.append("numeroFactura", String(filtros.numeroFactura));
        if (filtros.fechaDesde)
            params.append("fechaDesde", filtros.fechaDesde);
        if (filtros.fechaHasta)
            params.append("fechaHasta", filtros.fechaHasta);
        if (filtros.cliente)
            params.append("cliente", filtros.cliente);
        if (filtros.estado)
            params.append("estado", filtros.estado);
        if (filtros.informadaArca !== undefined)
            params.append("informadaArca", String(filtros.informadaArca));
        params.append("page", String(filtros.page || 0));
        params.append("size", String(filtros.size || 5));
        const res = await fetch(`${API_URL}?${params.toString()}`);
        if (!res.ok)
            throw new Error("Error al consultar facturas");
        return res.json();
    },
    buscarClientesExplorer: async (q) => {
        const res = await fetch(`${API_URL}/clientes/buscar?q=${encodeURIComponent(q || "")}`);
        return res.ok ? res.json() : [];
    },
    buscarArticulosExplorer: async (listaPrecioId, q) => {
        const res = await fetch(`${API_URL}/articulos/buscar?listaPrecioId=${listaPrecioId}&q=${encodeURIComponent(q || "")}`);
        return res.ok ? res.json() : [];
    },
    obtenerListasPrecio: async () => {
        const res = await fetch(`${API_URL}/listas-precio`);
        return res.ok ? res.json() : [];
    },
    obtenerCondicionesIva: async () => {
        const res = await fetch(`${API_URL}/condiciones-iva`);
        return res.ok ? res.json() : [];
    },
    obtenerPuntosVenta: async () => {
        const res = await fetch(`${API_URL}/puntos-venta`);
        return res.ok ? res.json() : [];
    },
    obtenerTiposMoneda: async () => {
        const res = await fetch(`${API_URL}/tipos-moneda`);
        return res.ok ? res.json() : [];
    },
};
