package(default_visibility = ["//visibility:public"])

load(
    "//dashboard/server:springboot.bzl",
    "add_boot_jetty_starter",
    "add_boot_web_starter",
    "springboot",
)

app_deps = [
    "@org_springframework_boot_spring_boot_starter//jar",
    "@org_springframework_boot_spring_boot//jar",
    "@org_springframework_spring_context//jar",
    "@org_springframework_boot_spring_boot_autoconfigure//jar",
    "@org_slf4j_jul_to_slf4j//jar",
    "@javax_annotation_javax_annotation_api//jar",
    "@org_springframework_spring_core//jar",
    "@org_springframework_spring_jcl//jar",
    "@org_yaml_snakeyaml//jar",
    "@io_springfox_springfox_swagger2//jar",
    "@io_swagger_swagger_annotations//jar",
    "@io_swagger_swagger_models//jar",
    "@com_fasterxml_jackson_core_jackson_annotations//jar",
    "@io_springfox_springfox_spi//jar",
    "@io_springfox_springfox_core//jar",
    "@io_springfox_springfox_schema//jar",
    "@io_springfox_springfox_swagger_common//jar",
    "@io_springfox_springfox_spring_web//jar",
    "@com_google_guava_guava//jar",
    "@com_fasterxml_classmate//jar",
    "@org_slf4j_slf4j_api//jar",
    "@org_springframework_plugin_spring_plugin_core//jar",
    "@org_springframework_spring_beans//jar",
    "@org_springframework_spring_aop//jar",
    "@org_springframework_plugin_spring_plugin_metadata//jar",
    "@org_mapstruct_mapstruct//jar",
    "@io_springfox_springfox_swagger_ui//jar",
    "@org_springframework_boot_spring_boot_starter_data_jpa//jar",
    "@org_springframework_boot_spring_boot_starter_aop//jar",
    "@org_aspectj_aspectjweaver//jar",
    "@org_springframework_boot_spring_boot_starter_jdbc//jar",
    "@org_springframework_spring_jdbc//jar",
    "@javax_transaction_javax_transaction_api//jar",
    "@javax_xml_bind_jaxb_api//jar",
    "@javax_activation_javax_activation_api//jar",
    "@org_hibernate_hibernate_core//jar",
    "@org_jboss_logging_jboss_logging//jar",
    "@javax_persistence_javax_persistence_api//jar",
    "@org_javassist_javassist//jar",
    "@net_bytebuddy_byte_buddy//jar",
    "@antlr_antlr//jar",
    "@org_jboss_jandex//jar",
    "@org_dom4j_dom4j//jar",
    "@org_hibernate_common_hibernate_commons_annotations//jar",
    "@org_springframework_data_spring_data_jpa//jar",
    "@org_springframework_data_spring_data_commons//jar",
    "@org_springframework_spring_orm//jar",
    "@org_springframework_spring_tx//jar",
    "@org_springframework_spring_aspects//jar",
    "@org_springframework_boot_spring_boot_starter_test//jar",
    "@org_springframework_boot_spring_boot_test//jar",
    "@org_springframework_boot_spring_boot_test_autoconfigure//jar",
    "@com_jayway_jsonpath_json_path//jar",
    "@net_minidev_json_smart//jar",
    "@net_minidev_accessors_smart//jar",
    "@org_ow2_asm_asm//jar",
    "@junit_junit//jar",
    "@org_assertj_assertj_core//jar",
    "@org_mockito_mockito_core//jar",
    "@net_bytebuddy_byte_buddy_agent//jar",
    "@org_objenesis_objenesis//jar",
    "@org_hamcrest_hamcrest_core//jar",
    "@org_hamcrest_hamcrest_library//jar",
    "@org_skyscreamer_jsonassert//jar",
    "@com_vaadin_external_google_android_json//jar",
    "@org_springframework_spring_test//jar",
    "@org_xmlunit_xmlunit_core//jar",
    "@org_springframework_boot_spring_boot_starter_web//jar",
    "@org_springframework_boot_spring_boot_starter_json//jar",
    "@com_fasterxml_jackson_core_jackson_databind//jar",
    "@com_fasterxml_jackson_core_jackson_core//jar",
    "@com_fasterxml_jackson_datatype_jackson_datatype_jdk8//jar",
    "@com_fasterxml_jackson_datatype_jackson_datatype_jsr310//jar",
    "@com_fasterxml_jackson_module_jackson_module_parameter_names//jar",
    "@org_springframework_boot_spring_boot_starter_tomcat//jar",
    "@org_apache_tomcat_embed_tomcat_embed_core//jar",
    "@org_apache_tomcat_embed_tomcat_embed_el//jar",
    "@org_apache_tomcat_embed_tomcat_embed_websocket//jar",
    "@org_hibernate_validator_hibernate_validator//jar",
    #"@javax_validation_validation_api//jar",
    "@org_springframework_spring_web//jar",
    "@org_springframework_spring_webmvc//jar",
    "@org_springframework_spring_expression//jar",
    "@com_h2database_h2//jar",
    "@org_apache_logging_log4j_log4j_api//jar",
    "@org_apache_logging_log4j_log4j_core//jar",
    "@org_apache_logging_log4j_log4j_slf4j_impl//jar",
]

#add_boot_jetty_starter(app_deps)
#add_boot_web_starter(app_deps)

springboot(
    name = "twister2-dash-server",
    boot_app_class = "edu.iu.dsc.tws.dashboard.DashboardApplication",
    deps = app_deps,
)
