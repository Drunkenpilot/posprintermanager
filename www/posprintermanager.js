var exec = require('cordova/exec');

exports.buildImage = function(args, success, error) {
    args = JSON.parse(args);
    printContent = args.printContent;
    printTemplate = args.printTemplate;
    printCanvas = [545, 0, 0, 15];
    exec(success, error, "posprintermanager", "buildImage", [
        [printContent], printTemplate, printCanvas
    ]);
};

exports.search = function(args, success, error) {
    args = JSON.parse(args);
    timeout = args.timeout;
    vendor = args.vendor;
    type = args.type;
    exec(success, error, "posprintermanager", "search", [
        timeout, vendor, type
    ]);
};

exports.print = function(args, success, error) {
    args = JSON.parse(args);
    vendor = args.vendor;
    printData = args.printData;
    printCanvas = args.printCanvas;
    pulse = args.pulse;
    model = args.model;
    lang = args.lang;
    address = args.address;
    exec(success, error, "posprintermanager", "print", [
        vendor, [printData], printCanvas, pulse, model, lang, address
    ]);
}