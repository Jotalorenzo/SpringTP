import { facturaService } from "./facturaService.js";
let detallesActuales = [];
let facturaCargadaActual = null;
let paginaActual = 0;
const tamanioPagina = 5;
document.addEventListener("DOMContentLoaded", async () => {
    await inicializarFormulario();
    configurarEventos();
    await cargarGrilla();
});
async function inicializarFormulario() {
    try {
        const [clientes, puntosVenta, monedas, articulos] = await Promise.all([
            facturaService.obtenerClientes(),
            facturaService.obtenerPuntosVenta(),
            facturaService.obtenerTiposMoneda(),
            facturaService.obtenerArticulos(),
        ]);
        llenarSelect("clienteSelect", clientes, "id", "denominacion");
        llenarSelect("puntoVentaSelect", puntosVenta, "id", "descripcion");
        llenarSelect("monedaSelect", monedas, "id", "denominacion");
        llenarSelect("articuloSelect", articulos, "id", "denominacion");
    }
    catch (error) {
        console.error("Error al cargar paramétricas:", error);
    }
}
function llenarSelect(selectId, items, valueProp, textProp) {
    const select = document.getElementById(selectId);
    select.innerHTML = '<option value="">Seleccione una opción...</option>';
    items.forEach((item) => {
        const option = document.createElement("option");
        option.value = item[valueProp];
        option.textContent = item[textProp] || `ID: ${item[valueProp]}`;
        select.appendChild(option);
    });
}
function configurarEventos() {
    document
        .getElementById("btnAgregarLinea")
        ?.addEventListener("click", agregarLinea);
    document
        .getElementById("btnGuardarFactura")
        ?.addEventListener("click", crearFactura);
    document
        .getElementById("btnActualizarFactura")
        ?.addEventListener("click", actualizarFactura);
    document
        .getElementById("btnAnularFactura")
        ?.addEventListener("click", anularFactura);
    document
        .getElementById("btnEliminarFactura")
        ?.addEventListener("click", eliminarFactura);
    document
        .getElementById("btnImprimirPdf")
        ?.addEventListener("click", imprimirFacturaPdf);
    document
        .getElementById("btnNuevaFactura")
        ?.addEventListener("click", resetearAModoNuevo);
    // Eventos de la Grilla y Filtros
    document.getElementById("btnFiltrar")?.addEventListener("click", () => {
        paginaActual = 0;
        cargarGrilla();
    });
    document
        .getElementById("btnLimpiarFiltros")
        ?.addEventListener("click", limpiarFiltros);
    document
        .getElementById("btnPaginaAnterior")
        ?.addEventListener("click", () => {
        if (paginaActual > 0) {
            paginaActual--;
            cargarGrilla();
        }
    });
    document
        .getElementById("btnPaginaSiguiente")
        ?.addEventListener("click", () => {
        paginaActual++;
        cargarGrilla();
    });
}
// ==========================================
// LÓGICA DE LA GRILLA Y FILTROS
// ==========================================
async function cargarGrilla() {
    const nroVal = document.getElementById("filtroNumero")
        .value;
    const clienteVal = document.getElementById("filtroCliente").value;
    const fechaDesdeVal = document.getElementById("filtroFechaDesde").value;
    const fechaHastaVal = document.getElementById("filtroFechaHasta").value;
    const estadoVal = document.getElementById("filtroEstado").value;
    const arcaVal = document.getElementById("filtroArca")
        .value;
    const filtros = {
        page: paginaActual,
        size: tamanioPagina,
    };
    if (nroVal)
        filtros.numeroFactura = parseInt(nroVal);
    if (clienteVal)
        filtros.cliente = clienteVal;
    if (fechaDesdeVal)
        filtros.fechaDesde = fechaDesdeVal;
    if (fechaHastaVal)
        filtros.fechaHasta = fechaHastaVal;
    if (estadoVal)
        filtros.estado = estadoVal;
    if (arcaVal !== "")
        filtros.informadaArca = arcaVal === "true";
    try {
        const respuesta = await facturaService.buscarFacturasPaginadas(filtros);
        renderizarGrilla(respuesta.contenido);
        // Actualizar barra de paginación
        document.getElementById("infoPaginacion").textContent =
            `Página ${respuesta.pagina + 1} de ${respuesta.totalPaginas || 1} (Total: ${respuesta.totalElementos} facturas)`;
        document.getElementById("btnPaginaAnterior").disabled = respuesta.pagina === 0;
        document.getElementById("btnPaginaSiguiente").disabled = respuesta.pagina >= respuesta.totalPaginas - 1;
    }
    catch (error) {
        console.error(error);
    }
}
function renderizarGrilla(facturas) {
    const tbody = document.querySelector("#tablaGrillaFacturas tbody");
    tbody.innerHTML = "";
    if (facturas.length === 0) {
        tbody.innerHTML =
            '<tr><td colspan="8" style="text-align: center; color: #94a3b8;">No se encontraron facturas con los filtros seleccionados.</td></tr>';
        return;
    }
    facturas.forEach((f) => {
        const tr = document.createElement("tr");
        const tieneCae = f.cae && f.cae.trim().length > 0;
        const esAnulada = f.estado === "ANULADA";
        tr.innerHTML = `
            <td><strong>#${f.id}</strong></td>
            <td>${f.numeroFactura || "<em>Borrador</em>"}</td>
            <td>${f.fechaEmision}</td>
            <td>${f.clienteDenominacion}</td>
            <td>$${f.importeTotal.toFixed(2)}</td>
            <td>${esAnulada ? '<span class="tag-anulada">ANULADA</span>' : '<span style="color:#0369a1; font-weight:600;">EMITIDA</span>'}</td>
            <td>${tieneCae ? '<span class="tag-arca">ARCA</span>' : '<span class="tag-no-arca">Sin CAE</span>'}</td>
            <td>
                <button type="button" class="btn-outline" style="padding: 4px 8px; font-size: 12px;" onclick="cargarFacturaPorId(${f.id})">Ver / Editar</button>
                <button type="button" class="btn-outline" style="padding: 4px 8px; font-size: 12px;" onclick="imprimirPdfDirecto(${f.id})">📄 PDF</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}
function limpiarFiltros() {
    document.getElementById("filtroNumero").value = "";
    document.getElementById("filtroCliente").value = "";
    document.getElementById("filtroFechaDesde").value = "";
    document.getElementById("filtroFechaHasta").value = "";
    document.getElementById("filtroEstado").value = "";
    document.getElementById("filtroArca").value = "";
    paginaActual = 0;
    cargarGrilla();
}
// Hacemos globales las funciones para los botones de la grilla
window.cargarFacturaPorId = async (id) => {
    try {
        const f = await facturaService.obtenerFacturaPorId(id);
        facturaCargadaActual = f;
        document.getElementById("numeroFacturaInput").value =
            f.numero ? f.numero.toString() : "";
        document.getElementById("clienteSelect").value =
            f.clienteId ? f.clienteId.toString() : "";
        document.getElementById("puntoVentaSelect").value =
            f.puntoVentaId.toString();
        document.getElementById("monedaSelect").value =
            f.tipoMonedaId.toString();
        detallesActuales = f.detalles.map((d) => ({
            articuloId: d.articuloId,
            cantidad: d.cantidad,
            porcentajeBonificacion: 0.0,
        }));
        document.getElementById("tituloFormulario").textContent =
            `Modificando Factura #${f.id}`;
        aplicarReglasDeNegocioVisuales(f);
        // Desplazar la vista al formulario
        document
            .getElementById("seccionFormulario")
            ?.scrollIntoView({ behavior: "smooth" });
    }
    catch (error) {
        alert(error.message);
    }
};
window.imprimirPdfDirecto = async (id) => {
    try {
        const blob = await facturaService.descargarFacturaIndividualPdf(id);
        const fileUrl = URL.createObjectURL(blob);
        window.open(fileUrl, "_blank");
    }
    catch (error) {
        alert(error.message);
    }
};
// ==========================================
// DETALLES Y FORMULARIO
// ==========================================
function agregarLinea() {
    const articuloSelect = document.getElementById("articuloSelect");
    const cantidadInput = document.getElementById("cantidadInput");
    const articuloId = parseInt(articuloSelect.value);
    const cantidad = parseFloat(cantidadInput.value);
    if (isNaN(articuloId) || isNaN(cantidad) || cantidad <= 0) {
        alert("Seleccione un artículo y cantidad mayor a 0.");
        return;
    }
    detallesActuales.push({ articuloId, cantidad, porcentajeBonificacion: 0.0 });
    actualizarTablaUI();
}
function actualizarTablaUI() {
    const tbody = document.querySelector("#tablaDetalles tbody");
    tbody.innerHTML = "";
    const bloqueado = facturaCargadaActual &&
        (facturaCargadaActual.cae || facturaCargadaActual.estado === "ANULADA");
    detallesActuales.forEach((detalle, index) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${detalle.articuloId}</td>
            <td>${detalle.cantidad}</td>
            <td>${bloqueado ? "—" : `<button type="button" class="btn-outline" style="padding: 4px 8px;" onclick="eliminarLinea(${index})">Quitar</button>`}</td>
        `;
        tbody.appendChild(tr);
    });
}
window.eliminarLinea = (index) => {
    detallesActuales.splice(index, 1);
    actualizarTablaUI();
};
function armarPayload() {
    const clienteId = parseInt(document.getElementById("clienteSelect").value);
    const puntoVentaId = parseInt(document.getElementById("puntoVentaSelect").value);
    const tipoMonedaId = parseInt(document.getElementById("monedaSelect").value);
    const numeroInput = document.getElementById("numeroFacturaInput").value;
    if (isNaN(puntoVentaId) || isNaN(tipoMonedaId)) {
        alert("Seleccione Punto de Venta y Moneda.");
        return null;
    }
    if (detallesActuales.length === 0) {
        alert("Agregue al menos un artículo a la factura.");
        return null;
    }
    const hoy = new Date().toISOString().split("T")[0];
    return {
        numero: numeroInput ? parseInt(numeroInput) : undefined,
        fechaEmision: facturaCargadaActual
            ? facturaCargadaActual.fechaEmision
            : hoy,
        puntoVentaId,
        condicionIvaId: 1,
        tipoMonedaId,
        clienteId: isNaN(clienteId) ? null : clienteId,
        detalles: detallesActuales,
    };
}
async function crearFactura() {
    const payload = armarPayload();
    if (!payload)
        return;
    try {
        const res = await facturaService.crearFactura(payload);
        alert(`¡Factura creada con éxito! ID: ${res.id} - Número: ${res.numero || "Borrador"}`);
        resetearAModoNuevo();
        await cargarGrilla();
    }
    catch (error) {
        alert(error.message);
    }
}
async function actualizarFactura() {
    if (!facturaCargadaActual)
        return;
    const payload = armarPayload();
    if (!payload)
        return;
    try {
        await facturaService.actualizarFactura(facturaCargadaActual.id, payload);
        alert(`¡Factura #${facturaCargadaActual.id} actualizada con éxito!`);
        await cargarGrilla();
    }
    catch (error) {
        alert(error.message);
    }
}
async function anularFactura() {
    if (!facturaCargadaActual)
        return;
    if (!confirm(`¿Está seguro de ANULAR la factura ID #${facturaCargadaActual.id}?`))
        return;
    try {
        await facturaService.anularFactura(facturaCargadaActual.id);
        alert(`Factura #${facturaCargadaActual.id} ANULADA correctamente.`);
        window.cargarFacturaPorId(facturaCargadaActual.id);
        await cargarGrilla();
    }
    catch (error) {
        alert(error.message);
    }
}
async function eliminarFactura() {
    if (!facturaCargadaActual)
        return;
    if (!confirm(`¿Eliminar físicamente la factura ID #${facturaCargadaActual.id}?`))
        return;
    try {
        await facturaService.eliminarFactura(facturaCargadaActual.id);
        alert(`Factura eliminada de la base de datos.`);
        resetearAModoNuevo();
        await cargarGrilla();
    }
    catch (error) {
        alert(error.message);
    }
}
async function imprimirFacturaPdf() {
    if (!facturaCargadaActual)
        return;
    window.imprimirPdfDirecto(facturaCargadaActual.id);
}
function aplicarReglasDeNegocioVisuales(f) {
    const alerta = document.getElementById("alertaEstado");
    const btnGuardar = document.getElementById("btnGuardarFactura");
    const btnActualizar = document.getElementById("btnActualizarFactura");
    const btnAnular = document.getElementById("btnAnularFactura");
    const btnEliminar = document.getElementById("btnEliminarFactura");
    const btnImprimir = document.getElementById("btnImprimirPdf");
    const btnNuevo = document.getElementById("btnNuevaFactura");
    btnGuardar.style.display = "none";
    btnImprimir.style.display = "inline-block";
    btnNuevo.style.display = "inline-block";
    alerta.style.display = "block";
    const tieneCae = f.cae && f.cae.trim().length > 0;
    const tieneNumero = f.numero && f.numero > 0;
    const esAnulada = f.estado === "ANULADA";
    if (esAnulada) {
        alerta.className = "badge badge-anulada";
        alerta.textContent = `FACTURA ANULADA (ID #${f.id}). Bloqueada para modificaciones.`;
        btnActualizar.style.display = "none";
        btnAnular.style.display = "none";
        btnEliminar.style.display = "none";
        bloquearControles(true);
    }
    else if (tieneCae) {
        alerta.className = "badge badge-arca";
        alerta.textContent = `INFORMADA A ARCA (ID #${f.id} - CAE: ${f.cae}). Modificación bloqueada. Solo se permite Anular.`;
        btnActualizar.style.display = "none";
        btnEliminar.style.display = "none";
        btnAnular.style.display = "inline-block";
        bloquearControles(true);
    }
    else if (tieneNumero) {
        alerta.className = "badge badge-emitida";
        alerta.textContent = `Factura emitida con Número #${f.numero} (ID #${f.id}). No se puede eliminar, solo Anular o Modificar.`;
        btnActualizar.style.display = "inline-block";
        btnAnular.style.display = "inline-block";
        btnEliminar.style.display = "none";
        bloquearControles(false);
    }
    else {
        alerta.className = "badge badge-borrador";
        alerta.textContent = `Borrador sin número (ID #${f.id}). Se puede modificar o eliminar físicamente.`;
        btnActualizar.style.display = "inline-block";
        btnEliminar.style.display = "inline-block";
        btnAnular.style.display = "none";
        bloquearControles(false);
    }
    actualizarTablaUI();
}
function bloquearControles(bloquear) {
    document.getElementById("clienteSelect").disabled =
        bloquear;
    document.getElementById("puntoVentaSelect").disabled =
        bloquear;
    document.getElementById("monedaSelect").disabled =
        bloquear;
    document.getElementById("numeroFacturaInput").disabled =
        bloquear;
    document.getElementById("articuloSelect").disabled =
        bloquear;
    document.getElementById("cantidadInput").disabled =
        bloquear;
    document.getElementById("btnAgregarLinea").disabled =
        bloquear;
}
function resetearAModoNuevo() {
    facturaCargadaActual = null;
    detallesActuales = [];
    document.getElementById("tituloFormulario").textContent =
        "Nueva Factura de Venta";
    document.getElementById("numeroFacturaInput").value =
        "";
    document.getElementById("clienteSelect").value = "";
    document.getElementById("puntoVentaSelect").value = "";
    document.getElementById("monedaSelect").value = "";
    document.getElementById("cantidadInput").value = "1";
    document.getElementById("alertaEstado").style.display = "none";
    document.getElementById("btnGuardarFactura").style.display = "inline-block";
    document.getElementById("btnActualizarFactura").style.display = "none";
    document.getElementById("btnAnularFactura").style.display = "none";
    document.getElementById("btnEliminarFactura").style.display = "none";
    document.getElementById("btnImprimirPdf").style.display = "none";
    document.getElementById("btnNuevaFactura").style.display = "none";
    bloquearControles(false);
    actualizarTablaUI();
}
