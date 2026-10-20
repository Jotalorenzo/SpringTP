import { facturaService } from './facturaService.js';
import { FacturaRequestDTO, FacturaDetalleRequestDTO } from './types';

// Estado local de la factura en memoria
let detallesActuales: FacturaDetalleRequestDTO[] = [];

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

    } catch (error) {
        console.error("Error al cargar paramétricas:", error);
        alert("Hubo un error al conectar con el servidor para cargar los datos.");
    }
}

function llenarSelect(selectId: string, items: any[], valueProp: string, textProp: string) {
    const select = document.getElementById(selectId) as HTMLSelectElement;
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
    const articuloSelect = document.getElementById('articuloSelect') as HTMLSelectElement;
    const cantidadInput = document.getElementById('cantidadInput') as HTMLInputElement;

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
    const tbody = document.querySelector('#tablaDetalles tbody') as HTMLTableSectionElement;
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
(window as any).eliminarLinea = (index: number) => {
    detallesActuales.splice(index, 1);
    actualizarTablaUI();
};

async function enviarFactura() {
    const clienteId = parseInt((document.getElementById('clienteSelect') as HTMLSelectElement).value);
    const puntoVentaId = parseInt((document.getElementById('puntoVentaSelect') as HTMLSelectElement).value);
    const tipoMonedaId = parseInt((document.getElementById('monedaSelect') as HTMLSelectElement).value);

    if (isNaN(clienteId) || isNaN(puntoVentaId) || isNaN(tipoMonedaId)) {
        alert("Debe completar todos los datos de la cabecera.");
        return;
    }

    if (detallesActuales.length === 0) {
        alert("La factura debe tener al menos un detalle.");
        return;
    }

    // Armar el payload idéntico a tu FacturaRequestDTO
    const payload: FacturaRequestDTO = {
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
    } catch (error) {
        console.error(error);
        alert("Ocurrió un error al guardar la factura.");
    }
}