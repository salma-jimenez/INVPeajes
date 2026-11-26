#---

### Diagrama de jerarquía (`hierarchy-diagram.md`)

```markdown
# Hierarchy Diagram — INVPeajes

```mermaid
flowchart TD
    APP["INVPeajes (com.invpeajes)"]

    APP --> UI["ui (User Interface & Navigation)"]
    APP --> SVC["service (Business Services)"]
    APP --> PER["persistence (Data Persistence & DB)"]
    APP --> MOD["model (Domain Entities)"]

    UI --> UI_MAIN["VentanaPrincipal, BarraLateral, Tema"]
    UI --> UI_PANELS["PanelInicio, PanelInventario, PanelDetallePeaje, PanelEnConstruccion"]
    UI --> UI_DATA["MapaPeajes, DatosPeajes, FuenteDatos, FuenteDatosOracle"]

    SVC --> SVC_TOLL["PeajeService, TicketService, TollRatesService"]
    SVC --> SVC_VEH["VehicleService, VehicleCategoryService"]
    SVC --> SVC_AST["AssistanceService, AssistanceResourceService"]
    SVC --> SVC_ERR["ValidationException"]

    PER --> PER_CONN["CrudDAO, DatabaseConnection, VehicleConnectionFactory, JdbcUtils, DataAccessException"]
    PER --> PER_TOLL["IPeajeDAO / PeajeDAOImpl, ICasetaDAO / CasetaDAOImpl, TicketDao, TollRateDao, TollUserDao"]
    PER --> PER_GEO["IRutaDAO / RutaDAOImpl, ISectorDAO / SectorDAOImpl, IDepartamentoDAO / DepartamentoDAOImpl, IConcesionarioDAO / ConcesionarioDAOImpl, IIntermediadorDAO / IntermediadorDAOImpl"]
    PER --> PER_VEH["IVehicleDAO / VehicleDAOImpl, IVehicleCategoryDAO / VehicleCategoryDAOImpl"]
    PER --> PER_AST["IAssistanceDAO / AssistanceDAOImpl, IAssistanceHistoryDAO / AssistanceHistoryDAOImpl, IAssistanceResourceDAO / AssistanceResourceDAOImpl"]

    MOD --> MOD_TOLL["Peaje -> PeajeElectronico, PeajeManual | Caseta | Ticket | TollRate | TollUser"]
    MOD --> MOD_VEH["Vehicle | VehicleCategory"]
    MOD --> MOD_AST["Assistance | AssistanceHistory | AssistanceResource | AssistanceStatus | AssistanceType | ResourceAvailability"]
    MOD --> MOD_GEO["Ruta | Sector | Departamento | Municipio | Concesionario | Intermediador"]