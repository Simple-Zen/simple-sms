import 'package:simple_sms/src/models/query_obj.dart';

abstract class Query {
  // Contact(s)
  static List<Map<Object?, Object?>> query(QueryObj query) {
    return [];
  }
}
  