import { facturaService } from "./facturaService.js";
import { FacturaRequestDTO, FacturaDetalleRequestDTO } from "./types.js";

let detallesActuales: FacturaDetalleRequestDTO[] = [];

document.addEventListener("DOMContentLoaded", async () => {
  await inicializarFormulario();
  configurarEventos();
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
  } catch (error) {
    console.error("Error al cargar paramétricas:", error);
    alert("Hubo un error al conectar con el servidor para cargar los datos.");
  }
}

function llenarSelect(
  selectId: string,
  items: any[],
  valueProp: string,
  textProp: string,
) {
  const select = document.getElementById(selectId) as HTMLSelectElement;
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
    ?.addEventListener("click", enviarFactura);
}

function agregarLinea() {
  const articuloSelect = document.getElementById(
    "articuloSelect",
  ) as HTMLSelectElement;
  const cantidadInput = document.getElementById(
    "cantidadInput",
  ) as HTMLInputElement;

  const articuloId = parseInt(articuloSelect.value);
  const cantidad = parseFloat(cantidadInput.value);

  if (isNaN(articuloId) || isNaN(cantidad) || cantidad <= 0) {
    alert("Seleccione un artículo válido y una cantidad mayor a 0.");
    return;
  }

  detallesActuales.push({
    articuloId: articuloId,
    cantidad: cantidad,
    porcentajeBonificacion: 0.0,
  });

  actualizarTablaUI();
}

function actualizarTablaUI() {
  const tbody = document.querySelector(
    "#tablaDetalles tbody",
  ) as HTMLTableSectionElement;
  tbody.innerHTML = "";

  detallesActuales.forEach((detalle, index) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
            <td>${detalle.articuloId}</td>
            <td>${detalle.cantidad}</td>
            <td><button type="button" onclick="eliminarLinea(${index})">Eliminar</button></td>
        `;
    tbody.appendChild(tr);
  });
}

(window as any).eliminarLinea = (index: number) => {
  detallesActuales.splice(index, 1);
  actualizarTablaUI();
};

async function enviarFactura() {
  const clienteId = parseInt(
    (document.getElementById("clienteSelect") as HTMLSelectElement).value,
  );
  const puntoVentaId = parseInt(
    (document.getElementById("puntoVentaSelect") as HTMLSelectElement).value,
  );
  const tipoMonedaId = parseInt(
    (document.getElementById("monedaSelect") as HTMLSelectElement).value,
  );

  if (isNaN(puntoVentaId) || isNaN(tipoMonedaId)) {
    alert("Debe seleccionar Punto de Venta y Moneda.");
    return;
  }

  if (detallesActuales.length === 0) {
    alert("La factura debe tener al menos un detalle.");
    return;
  }

  // Generar datos obligatorios requeridos por Spring
  const hoy = new Date().toISOString().split("T")[0];
  const numeroAleatorio = Math.floor(1000 + Math.random() * 9000);

  const payload: FacturaRequestDTO = {
    numero: numeroAleatorio,
    fechaEmision: hoy,
    puntoVentaId: puntoVentaId,
    condicionIvaId: 1, // ID 1 = Responsable Inscripto cargado por el Seeder
    tipoMonedaId: tipoMonedaId,
    clienteId: isNaN(clienteId) ? null : clienteId,
    detalles: detallesActuales,
  };

  try {
    const respuesta = await facturaService.crearFactura(payload);
    alert(
      `¡Factura generada exitosamente con Número: ${respuesta.numeroFactura}! Total: $${respuesta.importeTotal}`,
    );
    detallesActuales = [];
    actualizarTablaUI();
  } catch (error: any) {
    console.error(error);
    alert(`Ocurrió un error al guardar la factura: ${error.message}`);
  }
}
