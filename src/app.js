import { facturaService } from './facturaService.js';
let detallesActuales = [];
let facturaSeleccionadaId = null;
let paginaActual = 0;
document.addEventListener('DOMContentLoaded', async () => {
    await inicializarFormulario();
    configurarEventos();
    await cargarGrillaFacturas();
});
async function inicializarFormulario() {
    try {
        const [clientes, puntosVenta, monedas, articulos] = await Promise.all([
            facturaService.obtenerClientes(),
            facturaService.obtenerPuntosVenta(),
            facturaService.obtenerTiposMoneda(),
            facturaService.obtenerArticulos()
        ]);
        llenarSelect('clienteSelect', clientes, 'id', 'denominacion');
        llenarSelect('puntoVentaSelect', puntosVenta, 'id', 'descripcion');
        llenarSelect('monedaSelect', monedas, 'id', 'denominacion');
        llenarSelect('articuloSelect', articulos, 'id', 'denominacion');
    }
    catch (error) {
        console.error("Error al cargar paramétricas:", error);
    }
}
function llenarSelect(selectId, items, valueProp, textProp) {
    const select = document.getElementById(selectId);
    if (!select)
        return;
    select.innerHTML = '<option value="">Seleccione una opción...</option>';
    items.forEach(item => {
        const option = document.createElement('option');
        option.value = String(item[valueProp]);
        option.textContent = item[textProp] || `ID: ${item[valueProp]}`;
        select.appendChild(option);
    });
}
function configurarEventos() {
    document.getElementById('btnAgregarLinea')?.addEventListener('click', agregarLinea);
    document.getElementById('btnGuardarFactura')?.addEventListener('click', enviarFactura);
    document.getElementById('btnFiltrar')?.addEventListener('click', () => { paginaActual = 0; cargarGrillaFacturas(); });
    document.getElementById('btnLimpiarFiltros')?.addEventListener('click', limpiarFiltros);
    document.getElementById('btnPaginaAnterior')?.addEventListener('click', () => { if (paginaActual > 0) {
        paginaActual--;
        cargarGrillaFacturas();
    } });
    document.getElementById('btnPaginaSiguiente')?.addEventListener('click', () => { paginaActual++; cargarGrillaFacturas(); });
    document.getElementById('btnInformarArca')?.addEventListener('click', informarArca);
    document.getElementById('btnAnularFactura')?.addEventListener('click', anularFactura);
    document.getElementById('btnEliminarFactura')?.addEventListener('click', eliminarFactura);
    document.getElementById('btnImprimirPdf')?.addEventListener('click', imprimirPdf);
    document.getElementById('btnNuevaFactura')?.addEventListener('click', resetearFormulario);
}
function agregarLinea() {
    const articuloSelect = document.getElementById('articuloSelect');
    const cantidadInput = document.getElementById('cantidadInput');
    const articuloId = parseInt(articuloSelect.value);
    const cantidad = parseInt(cantidadInput.value);
    if (isNaN(articuloId) || isNaN(cantidad) || cantidad <= 0) {
        alert("Seleccione un artículo válido y una cantidad mayor a 0.");
        return;
    }
    detallesActuales.push({ articuloId, cantidad });
    actualizarTablaUI();
}
function actualizarTablaUI() {
    const tbody = document.querySelector('#tablaDetalles tbody');
    tbody.innerHTML = '';
    detallesActuales.forEach((detalle, index) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${detalle.articuloId}</td>
            <td>${detalle.cantidad}</td>
            <td><button type="button" class="btn-danger" style="padding: 4px 8px;" onclick="eliminarLinea(${index})">Quitar</button></td>
        `;
        tbody.appendChild(tr);
    });
}
window.eliminarLinea = (index) => {
    detallesActuales.splice(index, 1);
    actualizarTablaUI();
};
async function enviarFactura() {
    const clienteId = parseInt(document.getElementById('clienteSelect').value);
    const puntoVentaId = parseInt(document.getElementById('puntoVentaSelect').value);
    const tipoMonedaId = parseInt(document.getElementById('monedaSelect').value);
    const numInput = document.getElementById('numeroFacturaInput').value;
    const numero = numInput ? parseInt(numInput) : undefined;
    if (isNaN(puntoVentaId) || isNaN(tipoMonedaId)) {
        alert("Punto de Venta y Moneda son obligatorios.");
        return;
    }
    if (detallesActuales.length === 0) {
        alert("La factura debe tener al menos un artículo.");
        return;
    }
    const payload = {
        numero,
        fechaEmision: new Date().toISOString().split('T')[0],
        condicionIvaId: 1,
        clienteId: isNaN(clienteId) ? null : clienteId,
        puntoVentaId,
        tipoMonedaId,
        detalles: detallesActuales,
    };
    try {
        const respuesta = await facturaService.crearFactura(payload);
        alert(`¡Factura creada exitosamente! ID: ${respuesta.id}`);
        resetearFormulario();
        await cargarGrillaFacturas();
    }
    catch (error) {
        alert("Error al guardar: " + error.message);
    }
}
async function cargarGrillaFacturas() {
    const filtroNumero = document.getElementById('filtroNumero').value;
    const filtroCliente = document.getElementById('filtroCliente').value;
    const filtroFechaDesde = document.getElementById('filtroFechaDesde').value;
    const filtroFechaHasta = document.getElementById('filtroFechaHasta').value;
    const filtroEstado = document.getElementById('filtroEstado').value;
    const filtroArca = document.getElementById('filtroArca').value;
    try {
        const pagina = await facturaService.buscarFacturasPaginadas({
            numeroFactura: filtroNumero ? parseInt(filtroNumero) : undefined,
            cliente: filtroCliente || undefined,
            fechaDesde: filtroFechaDesde || undefined,
            fechaHasta: filtroFechaHasta || undefined,
            estado: filtroEstado || undefined,
            informadaArca: filtroArca !== "" ? (filtroArca === "true") : undefined,
            page: paginaActual,
            size: 5
        });
        renderizarTablaFacturas(pagina.contenido);
        document.getElementById('infoPaginacion').textContent = `Página ${pagina.pagina + 1} de ${pagina.totalPaginas || 1}`;
    }
    catch (error) {
        console.error("Error al cargar facturas:", error);
    }
}
function renderizarTablaFacturas(facturas) {
    const tbody = document.querySelector('#tablaGrillaFacturas tbody');
    tbody.innerHTML = '';
    if (facturas.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align: center;">No se encontraron facturas.</td></tr>';
        return;
    }
    facturas.forEach(f => {
        const tr = document.createElement('tr');
        const badgeArca = f.cae
            ? `<span class="tag-arca">CAE: ${f.cae}</span>`
            : `<span class="tag-no-arca">Sin CAE</span>`;
        tr.innerHTML = `
            <td>${f.id}</td>
            <td>${f.numeroFactura || 'Borrador'}</td>
            <td>${f.fechaEmision}</td>
            <td>${f.clienteDenominacion}</td>
            <td>$${f.importeTotal.toFixed(2)}</td>
            <td><span class="${f.estado === 'ANULADA' ? 'tag-anulada' : ''}">${f.estado}</span></td>
            <td>${badgeArca}</td>
            <td><button class="btn-outline" style="padding: 4px 8px;" onclick="seleccionarFactura(${f.id})">Ver / Gestionar</button></td>
        `;
        tbody.appendChild(tr);
    });
}
window.seleccionarFactura = async (id) => {
    try {
        const factura = await facturaService.obtenerFacturaPorId(id);
        facturaSeleccionadaId = factura.id;
        document.getElementById('numeroFacturaInput').value = factura.numero ? String(factura.numero) : '';
        document.getElementById('clienteSelect').value = factura.clienteId ? String(factura.clienteId) : '';
        document.getElementById('puntoVentaSelect').value = String(factura.puntoVentaId);
        document.getElementById('monedaSelect').value = String(factura.tipoMonedaId);
        detallesActuales = factura.detalles.map(d => ({
            articuloId: d.articuloId,
            cantidad: d.cantidad,
            descripcion: d.descripcion
        }));
        actualizarTablaUI();
        // Mostrar botones de acción
        document.getElementById('btnGuardarFactura').style.display = 'none';
        document.getElementById('btnNuevaFactura').style.display = 'inline-block';
        document.getElementById('btnImprimirPdf').style.display = 'inline-block';
        const btnArca = document.getElementById('btnInformarArca');
        const btnAnular = document.getElementById('btnAnularFactura');
        const btnEliminar = document.getElementById('btnEliminarFactura');
        if (!factura.cae && factura.estado !== 'ANULADA') {
            btnArca.style.display = 'inline-block';
        }
        else {
            btnArca.style.display = 'none';
        }
        if (factura.estado !== 'ANULADA') {
            btnAnular.style.display = 'inline-block';
        }
        else {
            btnAnular.style.display = 'none';
        }
        if (!factura.numero && !factura.cae) {
            btnEliminar.style.display = 'inline-block';
        }
        else {
            btnEliminar.style.display = 'none';
        }
        document.getElementById('seccionFormulario').scrollIntoView({ behavior: 'smooth' });
    }
    catch (e) {
        alert("Error al cargar la factura: " + e.message);
    }
};
async function informarArca() {
    if (!facturaSeleccionadaId)
        return;
    if (!confirm("¿Desea enviar este comprobante a ARCA / AFIP para obtener el CAE oficial?"))
        return;
    try {
        const res = await facturaService.informarArca(facturaSeleccionadaId);
        alert(`¡Comprobante Autorizado por ARCA!\nCAE: ${res.cae}\nNúmero: ${res.numeroFactura}`);
        await window.seleccionarFactura(facturaSeleccionadaId);
        await cargarGrillaFacturas();
    }
    catch (err) {
        alert("Error de ARCA: " + err.message);
    }
}
async function anularFactura() {
    if (!facturaSeleccionadaId)
        return;
    if (!confirm("¿Confirma la anulación lógica de la factura?"))
        return;
    try {
        await facturaService.anularFactura(facturaSeleccionadaId);
        alert("Factura anulada.");
        await window.seleccionarFactura(facturaSeleccionadaId);
        await cargarGrillaFacturas();
    }
    catch (err) {
        alert("Error al anular: " + err.message);
    }
}
async function eliminarFactura() {
    if (!facturaSeleccionadaId)
        return;
    if (!confirm("¿Eliminar físicamente el borrador?"))
        return;
    try {
        await facturaService.eliminarFactura(facturaSeleccionadaId);
        alert("Factura eliminada.");
        resetearFormulario();
        await cargarGrillaFacturas();
    }
    catch (err) {
        alert("Error al eliminar: " + err.message);
    }
}
async function imprimirPdf() {
    if (!facturaSeleccionadaId)
        return;
    try {
        const blob = await facturaService.descargarFacturaIndividualPdf(facturaSeleccionadaId);
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
    }
    catch (err) {
        alert("Error al abrir PDF: " + err.message);
    }
}
function resetearFormulario() {
    facturaSeleccionadaId = null;
    detallesActuales = [];
    document.getElementById('numeroFacturaInput').value = '';
    actualizarTablaUI();
    document.getElementById('btnGuardarFactura').style.display = 'inline-block';
    document.getElementById('btnNuevaFactura').style.display = 'none';
    document.getElementById('btnInformarArca').style.display = 'none';
    document.getElementById('btnAnularFactura').style.display = 'none';
    document.getElementById('btnEliminarFactura').style.display = 'none';
    document.getElementById('btnImprimirPdf').style.display = 'none';
}
function limpiarFiltros() {
    document.getElementById('filtroNumero').value = '';
    document.getElementById('filtroCliente').value = '';
    document.getElementById('filtroFechaDesde').value = '';
    document.getElementById('filtroFechaHasta').value = '';
    document.getElementById('filtroEstado').value = '';
    document.getElementById('filtroArca').value = '';
    paginaActual = 0;
    cargarGrillaFacturas();
}
