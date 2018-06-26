package hu.trigary.simplenetty.serialization;

/**
 * A serializer and deserializer for all the data which is sent between the server and the client.
 * The implementation should be thread-safe.
 *
 * @param <D> the type of the data which can be processed
 */
public interface DataSerializer<D> {
	byte[] serialize(D data);
	
	/**
	 * Deserializes a single instance of the data from the provided byte array.
	 *
	 * @param bytes the serialized data
	 * @return the deserialized data
	 */
	D deserialize(byte[] bytes);
	
	/**
	 * Returns the type of the data which can be processed.
	 *
	 * @return the type of the data which can be processed
	 */
	Class<D> getType();
}
