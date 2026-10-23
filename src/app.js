import { facturaService } from './facturaService.js';
// Estado local de la factura en memoria
let detallesActuales = [];
document.addEventListener('DOMContentLoaded', async () => {
    await inicializarFormulario();
    configurarEventos();
});
async function inicializarFormulario() {
    try {
        // Carga en paralelo optimizada
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
        alert("Hubo un error al conectar con el servidor para cargar los datos.");
    }
}
function llenarSelect(selectId, items, valueProp, textProp) {
    const select = document.getElementById(selectId);
    select.innerHTML = '<option value="">Seleccione una opción...</option>';
    items.forEach(item => {
        const option = document.createElement('option');
        option.value = item[valueProp];
        option.textContent = item[textProp] || `ID: ${item[valueProp]}`;
        select.appendChild(option);
    });
}
function configurarEventos() {
    document.getElementById('btnAgregarLinea')?.addEventListener('click', agregarLinea);
    document.getElementById('btnGuardarFactura')?.addEventListener('click', enviarFactura);
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
    // Agregar al estado
    detallesActuales.push({
        articuloId: articuloId,
        cantidad: cantidad
    });
    actualizarTablaUI();
}
function actualizarTablaUI() {
    const tbody = document.querySelector('#tablaDetalles tbody');
    tbody.innerHTML = ''; // Limpiar
    detallesActuales.forEach((detalle, index) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${detalle.articuloId}</td>
            <td>${detalle.cantidad}</td>
            <td><button onclick="eliminarLinea(${index})">Eliminar</button></td>
        `;
        tbody.appendChild(tr);
    });
}
// Hacemos la función global para que pueda ser llamada desde el onclick del HTML generado
window.eliminarLinea = (index) => {
    detallesActuales.splice(index, 1);
    actualizarTablaUI();
};
async function enviarFactura() {
    const clienteId = parseInt(document.getElementById('clienteSelect').value);
    const puntoVentaId = parseInt(document.getElementById('puntoVentaSelect').value);
    const tipoMonedaId = parseInt(document.getElementById('monedaSelect').value);
    if (isNaN(clienteId) || isNaN(puntoVentaId) || isNaN(tipoMonedaId)) {
        alert("Debe completar todos los datos de la cabecera.");
        return;
    }
    if (detallesActuales.length === 0) {
        alert("La factura debe tener al menos un detalle.");
        return;
    }
    // Armar el payload idéntico a tu FacturaRequestDTO
    const payload = {
        numero: +1,                                          
        fechaEmision: new Date().toISOString().split('T')[0],       
        condicionIvaId: 1,                                          
        clienteId,
        puntoVentaId,
        tipoMonedaId,
        detalles: detallesActuales
    };
    try {
        const respuesta = await facturaService.crearFactura(payload);
        alert(`¡Factura generada exitosamente con ID: ${respuesta.id}!`);
        // Opcional: limpiar formulario después del éxito
        detallesActuales = [];
        actualizarTablaUI();
    }
    catch (error) {
        console.error(error);
        alert("Ocurrió un error al guardar la factura.");
    }
}
