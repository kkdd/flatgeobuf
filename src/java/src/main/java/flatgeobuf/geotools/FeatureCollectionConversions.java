package flatgeobuf.geotools;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import com.google.flatbuffers.FlatBufferBuilder;

import flatgeobuf.generated.*;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class FeatureCollectionConversions {

    public class ColumnMeta {
        public String name;
        public ColumnType type;
    }

    public class HeaderMeta {
        public String name;
        public GeometryType geometryType;
        public byte dimensions;
        public List<ColumnMeta> columns;
    }

    public static void write(SimpleFeatureCollection featureCollection,
            OutputStream outputStream) throws IOException {

        // TODO: if no features do not output

        SimpleFeatureType featureType = featureCollection.getSchema();
        byte geometryType = toGeometryType(featureType.getGeometryDescriptor().getType().getBinding());
        // TODO: determine dimensions from type
        byte dimensions = 2;

        byte[] headerBuffer = buildHeader(geometryType);
        outputStream.write(headerBuffer);

        try (FeatureIterator<SimpleFeature> iterator = featureCollection.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                byte[] featureBuffer = FeatureConversions.write(feature, geometryType, dimensions);
                outputStream.write(featureBuffer);
            }
        }
    }

    private static byte toGeometryType(Class<?> geometryClass) {
        if (geometryClass.isAssignableFrom(Point.class))
            return GeometryType.Point;
        else if (geometryClass.isAssignableFrom(MultiPoint.class))
            return GeometryType.MultiPoint;
        else if (geometryClass.isAssignableFrom(LineString.class))
            return GeometryType.LineString;
        else if (geometryClass.isAssignableFrom(MultiLineString.class))
            return GeometryType.MultiLineString;
        else if (geometryClass.isAssignableFrom(Polygon.class))
            return GeometryType.Polygon;
        else if (geometryClass.isAssignableFrom(MultiPolygon.class))
            return GeometryType.MultiPolygon;
        else
            throw new RuntimeException("Unknown geometry type");
    }

    private static byte[] buildHeader(byte geometryType) {
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        Header.startHeader(builder);
        Header.addGeometryType(builder, geometryType);
        int offset = Header.endHeader(builder);

        builder.finishSizePrefixed(offset);

        return builder.sizedByteArray();
    }
}