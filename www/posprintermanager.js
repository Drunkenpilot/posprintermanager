var exec = require('cordova/exec');

exports.buildImage = function(args, success, error) {
    args = JSON.parse(args);
    printContent = args.printContent;
    filename = args.filename;
    printCanvas = args.printCanvas;
    exec(success, error, "posprintermanager", "buildImage", [
        [printContent],  printCanvas, filename
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