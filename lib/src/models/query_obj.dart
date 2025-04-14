class QueryObj {
  QueryObj({
    required this.contentUri,
    this.projection,
    this.selection,
    this.selectionArgs,
    this.sortOrder,
    this.cancellationSignal,
  });

  String contentUri; // Will be converted to Uri on the flip side
  List<String>? projection;
  String? selection;
  List<String>? selectionArgs;
  String? sortOrder;
  String? cancellationSignal;
}

class QueryObj_Args {
  QueryObj_Args({
    required this.contentUri,
    this.projection,
    this.queryArgs,
    this.cancellationSignal,
  });

  String contentUri; // Will be converted to Uri on the flip side
  List<String>? projection;
  Map<String, String>? queryArgs;
  String? cancellationSignal;
}
