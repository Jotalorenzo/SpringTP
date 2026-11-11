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
        convertirSelectEnBuscable('clienteSelect', 'Buscar y seleccionar cliente...');
        convertirSelectEnBuscable('articuloSelect', 'Buscar y seleccionar artículo...');

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
        numero,
        fechaEmision: new Date().toISOString().split('T')[0],
        condicionIva: 1,
        clienteId,
        puntoVentaId,
        tipoMonedaId,
        detalles: detallesActuales,
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


function convertirSelectEnBuscable(selectId: string, placeholderTexto: string = 'Escribir para buscar...') {
    const select = document.getElementById(selectId) as HTMLSelectElement | null;
    if (!select) return;

    if (select.dataset.searchableReady === 'true') return;
    select.dataset.searchableReady = 'true';

    const wrapper = document.createElement('div');
    wrapper.style.position = 'relative';
    wrapper.style.width = '100%';

    const input = document.createElement('input');
    input.type = 'text';
    input.placeholder = placeholderTexto;
    input.autocomplete = 'off';
    input.style.width = '100%';
    input.style.boxSizing = 'border-box';
    input.style.paddingRight = '30px';
    input.style.cursor = 'text';

    const flecha = document.createElement('span');
    flecha.textContent = '▾';
    flecha.style.position = 'absolute';
    flecha.style.right = '10px';
    flecha.style.top = '50%';
    flecha.style.transform = 'translateY(-50%)';
    flecha.style.pointerEvents = 'none';
    flecha.style.color = '#555';

    const dropdown = document.createElement('div');
    dropdown.style.position = 'absolute';
    dropdown.style.top = '100%';
    dropdown.style.left = '0';
    dropdown.style.right = '0';
    dropdown.style.maxHeight = '200px';
    dropdown.style.overflowY = 'auto';
    dropdown.style.background = '#fff';
    dropdown.style.border = '1px solid #ccc';
    dropdown.style.borderRadius = '0 0 6px 6px';
    dropdown.style.boxShadow = '0 4px 10px rgba(0,0,0,0.12)';
    dropdown.style.zIndex = '9999';
    dropdown.style.display = 'none';

    select.parentNode?.insertBefore(wrapper, select);
    wrapper.appendChild(input);
    wrapper.appendChild(flecha);
    wrapper.appendChild(dropdown);
    wrapper.appendChild(select);
    select.style.display = 'none';

    const normalizar = (str: string) =>
        (str || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');

    const renderizarOpciones = (filtro: string = '') => {
        dropdown.innerHTML = '';
        const termino = normalizar(filtro.trim());

        const opcionesValidas = Array.from(select.options).filter(opt => opt.value !== '');
        const coincidencias = opcionesValidas.filter(opt =>
            normalizar(opt.textContent || '').includes(termino)
        );

        if (coincidencias.length === 0) {
            const vacio = document.createElement('div');
            vacio.textContent = 'Sin coincidencias';
            vacio.style.padding = '8px 12px';
            vacio.style.color = '#888';
            dropdown.appendChild(vacio);
            return;
        }

        coincidencias.forEach(opt => {
            const item = document.createElement('div');
            item.textContent = opt.textContent;
            item.style.padding = '8px 12px';
            item.style.cursor = 'pointer';
            item.style.borderBottom = '1px solid #f2f2f2';

            item.addEventListener('mouseenter', () => {
                item.style.background = '#eef4ff';
            });
            item.addEventListener('mouseleave', () => {
                item.style.background = '#fff';
            });

            item.addEventListener('mousedown', (e) => {
                e.preventDefault();
                select.value = opt.value;
                input.value = opt.textContent || '';
                dropdown.style.display = 'none';
                select.dispatchEvent(new Event('change'));
            });

            dropdown.appendChild(item);
        });
    };

    input.addEventListener('focus', () => {
        renderizarOpciones(input.value);
        dropdown.style.display = 'block';
    });

    input.addEventListener('click', () => {
        renderizarOpciones(input.value);
        dropdown.style.display = 'block';
    });

    input.addEventListener('input', () => {
        select.value = '';
        renderizarOpciones(input.value);
        dropdown.style.display = 'block';

        const termino = normalizar(input.value.trim());
        if (termino) {
            const primera = Array.from(select.options).find(
                opt => opt.value !== '' && normalizar(opt.textContent || '').includes(termino)
            );
            if (primera) {
                select.value = primera.value;
            }
        }
    });

    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            const termino = normalizar(input.value.trim());
            const primera = Array.from(select.options).find(
                opt => opt.value !== '' && normalizar(opt.textContent || '').includes(termino)
            );
            if (primera) {
                select.value = primera.value;
                input.value = primera.textContent || '';
                dropdown.style.display = 'none';
                select.dispatchEvent(new Event('change'));
            }
        }
    });

    document.addEventListener('click', (e) => {
        if (!wrapper.contains(e.target as Node)) {
            dropdown.style.display = 'none';
            const seleccionada = select.options[select.selectedIndex];
            if (seleccionada && seleccionada.value !== '') {
                input.value = seleccionada.textContent || '';
            } else {
                input.value = '';
            }
        }
    });
}